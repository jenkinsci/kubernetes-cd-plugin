/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsSnapshotTaker;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.microsoft.jenkins.azurecommons.remote.SSHClient;
import com.microsoft.jenkins.kubernetes.util.Constants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.remoting.Channel;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.logging.Logger;

public class KubeconfigCredentials extends BaseStandardCredentials implements AncestorAware {
    private static final long serialVersionUID = 1L;

    private final KubeconfigSource kubeconfigSource;

    private transient Item owner;

    @DataBoundConstructor
    public KubeconfigCredentials(CredentialsScope scope,
                                 String id,
                                 String description,
                                 KubeconfigSource kubeconfigSource) {
        super(scope, id, description);
        this.kubeconfigSource = kubeconfigSource;
    }

    public KubeconfigSource getKubeconfigSource() {
        return kubeconfigSource;
    }

    public String getContent() {
        if (kubeconfigSource != null) {
            if (kubeconfigSource instanceof AncestorAware) {
                ((AncestorAware) kubeconfigSource).bindToAncestor(owner);
            }
            return kubeconfigSource.getContent();
        }
        return "";
    }

    @Override
    public void bindToAncestor(Item o) {
        this.owner = o;
    }

    /**
     * If this instance is being serialized to a remote channel, e.g., via Callable, replace it with a snapshot version.
     *
     * @return the snapshot version of this credential if it is being serialized to remote node, or self if not.
     */
    private Object writeReplace() {
        if (Channel.current() == null) {
            return this;
        }
        if (kubeconfigSource == null || kubeconfigSource.isSnapshotSource()) {
            return this;
        }
        return CredentialsProvider.snapshot(this);
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        public DescriptorImpl() {
            load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Kubernetes configuration (kubeconfig)";
        }

        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public DescriptorExtensionList<KubeconfigSource, Descriptor<KubeconfigSource>> getKubeconfigSources() {
            return Jenkins.getInstance().getDescriptorList(KubeconfigSource.class);
        }
    }

    public abstract static class KubeconfigSource extends AbstractDescribableImpl<KubeconfigSource> {
        @Nonnull
        public abstract String getContent();

        /**
         * Returns {@code true} if and only if hte source is self contained.
         *
         * @return {@code true} if and only if hte source is self contained.
         */
        public boolean isSnapshotSource() {
            return false;
        }
    }

    public abstract static class KubeconfigSourceDescriptor extends Descriptor<KubeconfigSource> {
    }

    public static class DirectEntryKubeconfigSource extends KubeconfigSource implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Secret content;

        @DataBoundConstructor
        public DirectEntryKubeconfigSource(String content) {
            this.content = Secret.fromString(content);
        }

        @Nonnull
        @Override
        public String getContent() {
            return Secret.toString(content);
        }

        @Override
        public boolean isSnapshotSource() {
            return true;
        }

        @Extension
        public static class DescriptorImpl extends KubeconfigSourceDescriptor {
            @Nonnull
            @Override
            public String getDisplayName() {
                return "Enter directly";
            }
        }
    }

    public static class FileOnMasterKubeconfigSource extends KubeconfigSource {
        private static final Logger LOGGER = Logger.getLogger(FileOnMasterKubeconfigSource.class.getName());

        private final String kubeconfigFile;

        @DataBoundConstructor
        public FileOnMasterKubeconfigSource(String kubeconfigFile) {
            this.kubeconfigFile = kubeconfigFile;
        }

        @Nonnull
        @Override
        public String getContent() {
            if (kubeconfigFile != null) {
                File file = new File(kubeconfigFile);
                if (file.isFile()) {
                    try {
                        return FileUtils.readFileToString(file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            throw new IllegalArgumentException("The kubeconfig file path is not configured");
        }

        public String getKubeconfigFile() {
            return kubeconfigFile;
        }

        @Extension
        public static class DescriptorImpl extends KubeconfigSourceDescriptor {
            @Nonnull
            @Override
            public String getDisplayName() {
                return "From a file on the Jenkins master";
            }
        }
    }

    public static class FileOnKubernetesMasterKubeconfigSource extends KubeconfigSource implements AncestorAware {
        private final String server;
        private final String sshCredentialId;
        private String file;

        private transient Item owner;

        @DataBoundConstructor
        public FileOnKubernetesMasterKubeconfigSource(String server, String sshCredentialId, String file) {
            this.server = server;
            this.sshCredentialId = sshCredentialId;
            this.file = file;
        }

        @Override
        public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
        }

        public String getServer() {
            return server;
        }

        public String getHost() {
            String serverName = getServer();
            if (StringUtils.isBlank(serverName)) {
                return null;
            }
            int colonIndex = serverName.lastIndexOf(':');
            if (colonIndex >= 0) {
                return serverName.substring(0, colonIndex);
            }
            return serverName;
        }

        public int getPort() {
            String serverName = getServer();
            if (StringUtils.isBlank(serverName)) {
                return Constants.DEFAULT_SSH_PORT;
            }
            int colonIndex = serverName.indexOf(':');
            if (colonIndex >= 0) {
                return Integer.parseInt(serverName.substring(colonIndex + 1));
            }
            return Constants.DEFAULT_SSH_PORT;
        }

        public String getSshCredentialId() {
            return sshCredentialId;
        }

        public String getFile() {
            if (StringUtils.isBlank(file)) {
                return getDescriptor().getDefaultFile();
            }
            return file;
        }

        @DataBoundSetter
        public void setFile(String file) {
            if (StringUtils.isBlank(file) || getDescriptor().getDefaultFile().equals(file)) {
                this.file = null;
            } else {
                this.file = file;
            }
        }

        @Override
        public void bindToAncestor(Item o) {
            this.owner = o;
        }

        @Nonnull
        @Override
        public String getContent() {
            StandardUsernameCredentials creds = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            StandardUsernameCredentials.class,
                            owner,
                            ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList()),
                    CredentialsMatchers.withId(getSshCredentialId()));

            if (creds == null) {
                throw new IllegalArgumentException("Cannot find SSH credentials with ID " + getSshCredentialId());
            }

            try {
                SSHClient sshClient = new SSHClient(getHost(), getPort(), creds);
                try (SSHClient connected = sshClient.connect()) {
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        connected.copyFrom(getFile(), out);
                        return out.toString(Constants.DEFAULT_CHARSET);
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Extension
        public static class DescriptorImpl extends KubeconfigSourceDescriptor {
            @Nonnull
            @Override
            public String getDisplayName() {
                return "From a file on the Kubernetes master node";
            }

            public String getDefaultFile() {
                return ".kube/config";
            }

            public ListBoxModel doFillSshCredentialIdItems(@AncestorInPath Item owner) {
                StandardListBoxModel model = new StandardListBoxModel();
                model.includeEmptyValue();
                model.includeAs(ACL.SYSTEM, owner, StandardUsernameCredentials.class);
                return model;
            }
        }
    }

    @Extension
    public static class CredentialsSnapshotTakerImpl extends CredentialsSnapshotTaker<KubeconfigCredentials> {
        @Override
        public Class<KubeconfigCredentials> type() {
            return KubeconfigCredentials.class;
        }

        @Override
        public KubeconfigCredentials snapshot(KubeconfigCredentials credentials) {
            final KubeconfigSource source = credentials.getKubeconfigSource();
            if (source.isSnapshotSource()) {
                return credentials;
            }
            return new KubeconfigCredentials(credentials.getScope(), credentials.getId(), credentials.getDescription(),
                    new DirectEntryKubeconfigSource(credentials.getContent()));
        }
    }
}

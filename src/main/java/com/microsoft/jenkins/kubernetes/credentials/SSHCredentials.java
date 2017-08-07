/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.microsoft.jenkins.azurecommons.remote.SSHClient;
import com.microsoft.jenkins.kubernetes.Messages;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.OutputStream;
import java.util.Collections;

public class SSHCredentials extends AbstractDescribableImpl<SSHCredentials> implements ConfigFileProvider {
    private String sshServer;
    private String sshCredentialsId;

    @DataBoundConstructor
    public SSHCredentials() {
    }

    public String getSshServer() {
        return sshServer;
    }

    @DataBoundSetter
    public void setSshServer(String sshServer) {
        this.sshServer = StringUtils.trimToEmpty(sshServer);
    }

    public String getSshCredentialsId() {
        return sshCredentialsId;
    }

    @DataBoundSetter
    public void setSshCredentialsId(String sshCredentialsId) {
        this.sshCredentialsId = sshCredentialsId;
    }

    public StandardUsernameCredentials getSshCredentials() {
        StandardUsernameCredentials creds = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernameCredentials.class,
                        Jenkins.getInstance(),
                        ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(getSshCredentialsId()));
        return creds;
    }

    public String getHost() {
        int colonIndex = sshServer.indexOf(':');
        if (colonIndex >= 0) {
            return sshServer.substring(0, colonIndex);
        }
        return sshServer;
    }

    public int getPort() {
        int colonIndex = sshServer.indexOf(':');
        if (colonIndex >= 0) {
            return Integer.parseInt(sshServer.substring(colonIndex));
        }
        return Constants.DEFAULT_SSH_PORT;
    }

    @Override
    public FilePath getConfigFilePath(FilePath workspace) throws Exception {
        SSHClient sshClient = new SSHClient(getHost(), getPort(), getSshCredentials());
        try (SSHClient ignore = sshClient.connect()) {
            FilePath configFile = workspace.createTempFile(Constants.KUBECONFIG_PREFIX, "");
            try (OutputStream out = configFile.write()) {
                sshClient.copyFrom(Constants.KUBECONFIG_FILE, out);
            }
            return configFile;
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<SSHCredentials> {
        public ListBoxModel doFillSshCredentialsIdItems(@AncestorInPath Item owner) {
            StandardListBoxModel model = new StandardListBoxModel();
            model.add(Messages.SSHCredentials_selectCredentials(), Constants.INVALID_OPTION);
            model.includeAs(ACL.SYSTEM, owner, SSHUserPrivateKey.class);
            model.includeAs(ACL.SYSTEM, owner, StandardUsernamePasswordCredentials.class);
            return model;
        }

        public FormValidation doCheckSshServer(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.SSHCredentials_serverRequired());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckSshCredentialsId(@QueryParameter String value) {
            if (StringUtils.isBlank(value) || Constants.INVALID_OPTION.equals(value)) {
                return FormValidation.error(Messages.SSHCredentials_credentialsIdRequired());
            }
            return FormValidation.ok();
        }
    }
}

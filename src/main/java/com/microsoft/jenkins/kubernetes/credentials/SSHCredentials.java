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
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.OutputStream;
import java.util.Collections;

public class SSHCredentials extends AbstractDescribableImpl<SSHCredentials> implements ConfigFileProvider {
    private String server;
    private String credentialsId;

    @DataBoundConstructor
    public SSHCredentials() {
    }

    public String getServer() {
        return server;
    }

    @DataBoundSetter
    public void setServer(String server) {
        this.server = StringUtils.trimToEmpty(server);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public StandardUsernameCredentials getSshCredentials() {
        StandardUsernameCredentials creds = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernameCredentials.class,
                        Jenkins.getInstance(),
                        ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(getCredentialsId()));
        return creds;
    }

    @Override
    public FilePath getConfigFilePath(FilePath workspace) throws Exception {
        String host;
        int port;
        int colonIndex = server.indexOf(':');
        if (colonIndex >= 0) {
            host = server.substring(0, colonIndex);
            port = Integer.parseInt(server.substring(colonIndex + 1));
        } else {
            host = server;
            port = Constants.DEFAULT_SSH_PORT;
        }
        SSHClient sshClient = new SSHClient(host, port, getSshCredentials());
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
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item owner) {
            StandardListBoxModel model = new StandardListBoxModel();
            model.add("--- Select SSH Credentials ---", Constants.INVALID_OPTION);
            model.includeAs(ACL.SYSTEM, owner, SSHUserPrivateKey.class);
            model.includeAs(ACL.SYSTEM, owner, StandardUsernamePasswordCredentials.class);
            return model;
        }
    }
}

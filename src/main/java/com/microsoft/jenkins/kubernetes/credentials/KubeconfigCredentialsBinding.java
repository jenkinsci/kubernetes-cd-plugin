/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.Binding;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;

public class KubeconfigCredentialsBinding extends Binding {
    public static final String DEFAULT_VARIABLE_NAME = "KUBECONFIG_CONTENT";

    @DataBoundConstructor
    public KubeconfigCredentialsBinding(String variable, String credentialsId) {
        super(variable, credentialsId);
    }

    @Override
    protected Class type() {
        return KubeconfigCredentials.class;
    }

    @Override
    public SingleEnvironment bindSingle(@Nonnull Run build,
                                        @Nullable FilePath workspace,
                                        @Nullable Launcher launcher,
                                        @Nonnull TaskListener listener) throws IOException, InterruptedException {
        KubeconfigCredentials credentials  = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        KubeconfigCredentials.class,
                        build.getParent(),
                        ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(getCredentialsId()));
        if (credentials == null) {
            throw new IllegalStateException("Cannot find kubeconfig credentials with ID '" + getCredentialsId() + "'");
        }
        credentials.bindToAncestor(build.getParent());
        return new SingleEnvironment(credentials.getContent());
    }

    @Extension
    @Symbol("kubeconfigContent")
    public static class DescriptorImpl extends BindingDescriptor<KubeconfigCredentials> {
        @Override
        protected Class<KubeconfigCredentials> type() {
            return KubeconfigCredentials.class;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Kubeconfig Content";
        }

        @Override
        public boolean requiresWorkspace() {
            return false;
        }

        public String getDefaultVariableName() {
            return KubeconfigCredentialsBinding.DEFAULT_VARIABLE_NAME;
        }
    }
}

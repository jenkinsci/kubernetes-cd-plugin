/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.impl.AbstractOnDiskBinding;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KubeconfigFileCredentialsBinding extends AbstractOnDiskBinding<KubeconfigCredentials> {
    public static final int FILE_MASK = 0400;
    private static final String DEFAULT_VARIABLE_NAME = "KUBECONFIG";

    @DataBoundConstructor
    public KubeconfigFileCredentialsBinding(String variable, String credentialsId) {
        super(variable, credentialsId);
    }

    @Override
    protected FilePath write(KubeconfigCredentials credentials, FilePath dir) throws IOException, InterruptedException {
        FilePath secret = dir.child("kubeconfig");
        String content = credentials.getContent();
        secret.write(content, StandardCharsets.UTF_8.name());
        secret.chmod(FILE_MASK);
        return secret;
    }

    @Override
    protected Class<KubeconfigCredentials> type() {
        return KubeconfigCredentials.class;
    }


    @Symbol("kubeconfigFile")
    @Extension public static class DescriptorImpl extends BindingDescriptor<KubeconfigCredentials> {

        @Override protected Class<KubeconfigCredentials> type() {
            return KubeconfigCredentials.class;
        }

        @Nonnull
        @Override public String getDisplayName() {
            return "Kubeconfig File";
        }

        public String getDefaultVariableName() {
            return KubeconfigFileCredentialsBinding.DEFAULT_VARIABLE_NAME;
        }

    }


}

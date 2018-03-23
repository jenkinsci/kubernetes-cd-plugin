/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import com.microsoft.jenkins.kubernetes.Messages;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * @deprecated Use {@link KubeconfigCredentials}.
 */
@Deprecated
public class TextCredentials extends AbstractDescribableImpl<TextCredentials> implements ClientWrapperFactory.Builder {
    private String serverUrl;
    private String certificateAuthorityData;
    private String clientCertificateData;
    private String clientKeyData;

    @DataBoundConstructor
    public TextCredentials() {

    }

    public String getServerUrl() {
        return serverUrl;
    }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getCertificateAuthorityData() {
        return certificateAuthorityData;
    }

    @DataBoundSetter
    public void setCertificateAuthorityData(String certificateAuthorityData) {
        this.certificateAuthorityData = certificateAuthorityData;
    }

    public String getClientCertificateData() {
        return clientCertificateData;
    }

    @DataBoundSetter
    public void setClientCertificateData(String clientCertificateData) {
        this.clientCertificateData = clientCertificateData;
    }

    public String getClientKeyData() {
        return clientKeyData;
    }

    @DataBoundSetter
    public void setClientKeyData(String clientKeyData) {
        this.clientKeyData = clientKeyData;
    }

    @Override
    public ClientWrapperFactory buildClientWrapperFactory() {
        return new ClientWrapperFactoryImpl(
                getServerUrl(), getCertificateAuthorityData(), getClientCertificateData(), getClientKeyData());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<TextCredentials> {
        public FormValidation doCheckServerUrl(@QueryParameter String value) {
            value = StringUtils.trimToEmpty(value);
            if (value.isEmpty()) {
                return FormValidation.error(Messages.TextCredentials_serverUrlRequired());
            }
            if (!value.startsWith(Constants.HTTPS_PREFIX)) {
                return FormValidation.error(Messages.TextCredentials_serverUrlShouldBeHttps());
            }
            return FormValidation.ok();
        }

        public String getDefaultServerUrl() {
            return Constants.HTTPS_PREFIX;
        }
    }

    private static class ClientWrapperFactoryImpl implements ClientWrapperFactory {
        private static final long serialVersionUID = 1L;

        private final String serverUrl;
        private final String certificateAuthorityData;
        private final String clientCertificateData;
        private final String clientKeyData;

        ClientWrapperFactoryImpl(String serverUrl,
                                 String certificateAuthorityData,
                                 String clientCertificateData,
                                 String clientKeyData) {
            this.serverUrl = serverUrl;
            this.certificateAuthorityData = certificateAuthorityData;
            this.clientCertificateData = clientCertificateData;
            this.clientKeyData = clientKeyData;
        }

        @Override
        public KubernetesClientWrapper buildClient(FilePath workspace) throws Exception {
            return new KubernetesClientWrapper(
                    serverUrl, certificateAuthorityData, clientCertificateData, clientKeyData);
        }
    }
}

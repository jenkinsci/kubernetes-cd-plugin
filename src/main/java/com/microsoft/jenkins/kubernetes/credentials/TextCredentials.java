/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import com.microsoft.jenkins.kubernetes.Messages;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class TextCredentials extends AbstractDescribableImpl<TextCredentials> {
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

        public FormValidation doCheckCertificateAuthorityData(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.TextCredentials_certificateAuthorityDataRequired());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckClientCertificateData(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.TextCredentials_clientCertificateDataRequired());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckClientKeyData(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.TextCredentials_clientKeyDataRequired());
            }
            return FormValidation.ok();
        }

        public String getDefaultServerUrl() {
            return Constants.HTTPS_PREFIX;
        }
    }
}

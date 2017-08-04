/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class TextCredentials extends AbstractDescribableImpl<TextCredentials> {
    private String server;
    private String certificateAuthorityData;
    private String clientCertificateData;
    private String clientKeyData;

    @DataBoundConstructor
    public TextCredentials() {

    }

    public String getServer() {
        return server;
    }

    @DataBoundSetter
    public void setServer(String server) {
        this.server = server;
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
    }
}

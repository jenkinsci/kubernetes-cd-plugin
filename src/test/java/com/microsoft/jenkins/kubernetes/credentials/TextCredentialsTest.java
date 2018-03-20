/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.util.FormValidation;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link TextCredentials}.
 */
public class TextCredentialsTest {
    @Test
    public void testClientFactorySerialization() {
        TextCredentials credentials = new TextCredentials();
        credentials.setServerUrl("https://example.com");
        credentials.setCertificateAuthorityData("ABCDEF");
        credentials.setClientCertificateData("GHIJK");
        credentials.setClientKeyData("LMN");

        ClientWrapperFactory factory = credentials.buildClientWrapperFactory();
        byte[] bytes = SerializationUtils.serialize(factory);
        Object deserialized = SerializationUtils.deserialize(bytes);
        assertTrue(deserialized instanceof ClientWrapperFactory);
    }

    @Test
    public void testDescriptorCheckServerUrl() {
        TextCredentials.DescriptorImpl descriptor = new TextCredentials.DescriptorImpl();

        assertEquals(FormValidation.ok(), descriptor.doCheckServerUrl("https://example.com"));
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckServerUrl(null).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckServerUrl("   ").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckServerUrl("http://example.com").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckServerUrl("example.com").kind);
    }

    @Test
    public void testDescriptorDefaultServerUrl() {
        TextCredentials.DescriptorImpl descriptor = new TextCredentials.DescriptorImpl();

        assertEquals(Constants.HTTPS_PREFIX, descriptor.getDefaultServerUrl());
    }
}

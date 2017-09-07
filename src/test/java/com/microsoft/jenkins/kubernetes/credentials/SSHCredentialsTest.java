/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.util.FormValidation;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link SSHCredentials}.
 */
public class SSHCredentialsTest {
    @Test
    public void testGetHostPort() {
        SSHCredentials credentials = new SSHCredentials();
        credentials.setSshServer("example.com");
        assertEquals("example.com", credentials.getHost());
        assertEquals(Constants.DEFAULT_SSH_PORT, credentials.getPort());

        credentials.setSshServer("sample.com:1234");
        assertEquals("sample.com", credentials.getHost());
        assertEquals(1234, credentials.getPort());
    }

    @Test
    public void testClientFactorySerialization() {
        SSHCredentials credentials = spy(new SSHCredentials());
        doReturn(mock(StandardUsernameCredentials.class)).when(credentials).getSshCredentials();
        credentials.setSshServer("example.com:1234");
        ClientWrapperFactory factory = credentials.buildClientWrapperFactory();

        byte[] bytes = SerializationUtils.serialize(factory);
        Object deserialized = SerializationUtils.deserialize(bytes);
        assertTrue(deserialized instanceof ClientWrapperFactory);
    }

    @Test
    public void testDescriptorCheckSshServer() {
        SSHCredentials.DescriptorImpl descriptor = new SSHCredentials.DescriptorImpl();

        assertEquals(FormValidation.ok(), descriptor.doCheckSshServer("abc.def"));
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckSshServer(null).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckSshServer("").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckSshServer("   ").kind);
    }

    @Test
    public void testDescriptorCheckSshCredentialsId() {
        SSHCredentials.DescriptorImpl descriptor = new SSHCredentials.DescriptorImpl();

        assertEquals(FormValidation.ok(), descriptor.doCheckSshCredentialsId("abc"));
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckSshCredentialsId(null).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckSshCredentialsId("").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckSshCredentialsId("  ").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckSshCredentialsId(Constants.INVALID_OPTION).kind);
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import hudson.model.Item;
import hudson.util.FormValidation;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigFileCredentials}.
 */
public class ConfigFileCredentialsTest {
    @Test
    public void testDescriptorCheckPath() {
        ConfigFileCredentials.DescriptorImpl descriptor = new ConfigFileCredentials.DescriptorImpl();

        assertEquals(FormValidation.ok(), descriptor.doCheckPath("abc"));
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckPath(null).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckPath("  ").kind);
    }

    @Test
    public void checkClientFactorySerialization() {
        ConfigFileCredentials credentials = new ConfigFileCredentials();
        credentials.setPath("abc");
        ClientWrapperFactory factory = credentials.buildClientWrapperFactory(mock(Item.class));
        byte[] bytes = SerializationUtils.serialize(factory);
        Object deserialized = SerializationUtils.deserialize(bytes);
        assertTrue(deserialized instanceof ClientWrapperFactory);
    }
}

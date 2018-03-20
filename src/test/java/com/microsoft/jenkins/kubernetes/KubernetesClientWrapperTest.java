/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.EnvVars;
import io.fabric8.kubernetes.client.Config;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link KubernetesClientWrapper}.
 */
public class KubernetesClientWrapperTest {
    @Test
    public void testPrepareSecretName() throws Exception {
        final int lengthLimit = Constants.KUBERNETES_NAME_LENGTH_LIMIT;
        assertEquals("ab", KubernetesClientWrapper.prepareSecretName("$var", "abc", new EnvVars("var", "ab")));
        assertException(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                // length breach
                KubernetesClientWrapper.prepareSecretName(new String(new char[lengthLimit + 1]).replace('\0', 'a'), "abc", new EnvVars());
            }
        });
        assertException(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                // illegal name
                KubernetesClientWrapper.prepareSecretName("Abc", "abc", new EnvVars());
            }
        });
        assertTrue(KubernetesClientWrapper.prepareSecretName(null, null, new EnvVars()).startsWith(Constants.KUBERNETES_SECRET_NAME_PREFIX));
        assertTrue(KubernetesClientWrapper.prepareSecretName(null, "abcd", new EnvVars()).startsWith(Constants.KUBERNETES_SECRET_NAME_PREFIX + "abcd"));
        assertTrue(KubernetesClientWrapper.prepareSecretName(null, new String(new char[lengthLimit + 1]).replace('\0', 'a'), new EnvVars()).length() <= Constants.KUBERNETES_NAME_LENGTH_LIMIT);
    }

    private <T extends Exception> void assertException(Class<T> clazz, Runnable action) {
        try {
            action.run();
            fail();
        } catch (Exception e) {
            if (!clazz.isAssignableFrom(e.getClass())) {
                throw e;
            }
        }
    }
}

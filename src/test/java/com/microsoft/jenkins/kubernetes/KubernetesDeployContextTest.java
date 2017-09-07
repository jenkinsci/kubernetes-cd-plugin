/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.google.common.collect.ImmutableList;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.model.Item;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link KubernetesDeployContext}.
 */
public class KubernetesDeployContextTest {
    @Test
    public void testSecretNamespace() {
        KubernetesDeployContext context = new KubernetesDeployContext();
        assertEquals(Constants.DEFAULT_KUBERNETES_NAMESPACE, context.getSecretNamespace());

        context.setSecretNamespace(null);
        assertEquals(Constants.DEFAULT_KUBERNETES_NAMESPACE, context.getSecretNamespace());

        context.setSecretNamespace("   ");
        assertEquals(Constants.DEFAULT_KUBERNETES_NAMESPACE, context.getSecretNamespace());

        context.setSecretNamespace("abc");
        assertEquals("abc", context.getSecretNamespace());

        context.setSecretNamespace(Constants.DEFAULT_KUBERNETES_NAMESPACE);
        assertEquals(Constants.DEFAULT_KUBERNETES_NAMESPACE, context.getSecretNamespace());
    }

    @Test
    public void testDockerCredentials() {
        KubernetesDeployContext context = new KubernetesDeployContext();
        assertListEquals(ImmutableList.of(), context.getDockerCredentials());

        context.setDockerCredentials(ImmutableList.of(
                new DockerRegistryEndpoint("", "credentials-1")
        ));
        assertListEquals(ImmutableList.of(new DockerRegistryEndpoint("", "credentials-1")), context.getDockerCredentials());

        context.setDockerCredentials(ImmutableList.of(
                new DockerRegistryEndpoint("", "credentials-1"),
                new DockerRegistryEndpoint("acr.azurecr.io", "credentials-2")
        ));
        assertListEquals(
                ImmutableList.of(
                        new DockerRegistryEndpoint("", "credentials-1"),
                        new DockerRegistryEndpoint("http://acr.azurecr.io", "credentials-2")
                ), context.getDockerCredentials());
    }

    private <T> void assertListEquals(List<? extends T> expected, List<? extends T> actual) {
        assertNotNull(actual);
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testResolveEndpoints() throws Exception {
        DockerRegistryEndpoint endpoint1 = mock(DockerRegistryEndpoint.class);
        DockerRegistryToken token1 = mock(DockerRegistryToken.class);
        when(endpoint1.getEffectiveUrl()).thenReturn(new URL("https://index.docker.io/v1/"));
        when(endpoint1.getCredentialsId()).thenReturn("cred-1");
        when(endpoint1.getToken(any(Item.class))).thenReturn(token1);

        DockerRegistryEndpoint endpoint2 = mock(DockerRegistryEndpoint.class);
        DockerRegistryToken token2 = mock(DockerRegistryToken.class);
        when(endpoint2.getEffectiveUrl()).thenReturn(new URL("http://acr.azurecr.io"));
        when(endpoint2.getCredentialsId()).thenReturn("cred-2");
        when(endpoint2.getToken(any(Item.class))).thenReturn(token2);

        KubernetesDeployContext context = spy(new KubernetesDeployContext());
        assertTrue(context.resolveEndpoints(mock(Item.class)).isEmpty());

        doReturn(Arrays.asList(endpoint1, endpoint2)).when(context).getDockerCredentials();
        List<ResolvedDockerRegistryEndpoint> resolved = context.resolveEndpoints(mock(Item.class));
        assertEquals(2, resolved.size());
        assertEquals(new URL("https://index.docker.io/v1/"), resolved.get(0).getUrl());
        assertEquals(token1, resolved.get(0).getToken());
        assertEquals(new URL("http://acr.azurecr.io"), resolved.get(1).getUrl());
        assertEquals(token2, resolved.get(1).getToken());
    }
}

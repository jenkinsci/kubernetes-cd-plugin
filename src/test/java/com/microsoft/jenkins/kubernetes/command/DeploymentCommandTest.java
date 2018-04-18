package com.microsoft.jenkins.kubernetes.command;

import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DeploymentCommand}
 */
public class DeploymentCommandTest {
    private static final String FALLBACK_MASTER = "Unknown";

    @Test
    public void testGetMasterHost() {
        assertEquals(FALLBACK_MASTER, DeploymentCommand.getMasterHost(null));

        KubernetesClientWrapper wrapper = mock(KubernetesClientWrapper.class);
        assertEquals(FALLBACK_MASTER, DeploymentCommand.getMasterHost(wrapper));

        KubernetesClient client = mock(KubernetesClient.class);
        when(wrapper.getClient()).thenReturn(client);
        assertEquals(FALLBACK_MASTER, DeploymentCommand.getMasterHost(wrapper));

        URL url = mock(URL.class);
        final String host = "some.host";
        when(url.getHost()).thenReturn(host);
        when(client.getMasterUrl()).thenReturn(url);
        assertEquals(host, DeploymentCommand.getMasterHost(wrapper));
    }
}

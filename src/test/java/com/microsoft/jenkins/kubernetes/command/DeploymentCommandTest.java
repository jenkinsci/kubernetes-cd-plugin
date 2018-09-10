package com.microsoft.jenkins.kubernetes.command;

import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import io.kubernetes.client.ApiClient;
import org.junit.Test;

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

        ApiClient client = mock(ApiClient.class);
        when(wrapper.getClient()).thenReturn(client);
        assertEquals(FALLBACK_MASTER, DeploymentCommand.getMasterHost(wrapper));

        final String host = "some.host";
        when(client.getBasePath()).thenReturn(host);
        assertEquals(host, DeploymentCommand.getMasterHost(wrapper));
    }
}

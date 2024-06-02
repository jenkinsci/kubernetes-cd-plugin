/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.microsoft.jenkins.azurecommons.remote.SSHClient;
import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.microsoft.jenkins.kubernetes.integration.TestHelpers.loadProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class IntegrationTest {
    private static final Logger LOGGER = Logger.getLogger(IntegrationTest.class.getName());

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @ClassRule
    public static JenkinsRule j = new JenkinsRule() {{
        // disable the default 180s timeout
        timeout = -1;
    }};

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public Timeout globalTimeout = new Timeout(30, TimeUnit.MINUTES);

    public final String masterHost;
    public final int port;
    public final String adminUser;
    public final String privateKeyPath;

    public final String dockerRegistry;
    public final String dockerUsername;
    public final String dockerPassword;
    public final String dockerRepository;

    public File workspaceKubeConfig;

    public JsonNode config;

    public String serverUrl;
    public String certificateAuthorityData;
    public String clientCertificateData;
    public String clientKeyData;

    protected Run run;
    protected FilePath workspace;
    protected Launcher launcher;
    protected TaskListener taskListener;

    protected EnvVars envVars = new EnvVars();

    public IntegrationTest() {
        String host = loadProperty("KUBERNETES_CD_MASTER_HOST");
        String[] parts = host.split(",", 2);
        masterHost = StringUtils.trimToNull(parts[0]);
        port = parts.length == 2 ? Integer.parseInt(StringUtils.trimToNull(parts[1])) : 22;
        adminUser = loadProperty("KUBERNETES_CD_ADMIN_USER", "azureuser");
        privateKeyPath = loadProperty("KUBERNETES_CD_KEY_PATH", new File(System.getProperty("user.home"), ".ssh/id_rsa").getAbsolutePath());

        dockerRegistry = loadProperty("KUBERNETES_CD_DOCKER_REGISTRY");
        dockerUsername = loadProperty("KUBERNETES_CD_DOCKER_USERNAME");
        dockerPassword = loadProperty("KUBERNETES_CD_DOCKER_PASSWORD");
        dockerRepository = loadProperty("KUBERNETES_CD_DOCKER_REPOSITORY");

        Assert.assertTrue("Master host is not configured in environment", StringUtils.isNotEmpty(masterHost));
    }

    @Before
    public void setup() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);

        SSHClient client = new SSHClient(masterHost, port, adminUser, null, FileUtils.readFileToString(new File(privateKeyPath)));
        workspaceKubeConfig = tmpFolder.newFile();
        try (SSHClient connected = client.connect()) {
            try (OutputStream os = new FileOutputStream(workspaceKubeConfig)) {
                connected.copyFrom(".kube/config", os);
            }
        }
        try {
            config = YAML.readTree(workspaceKubeConfig);
        } catch (Exception e) {
            config = JSON.readTree(workspaceKubeConfig);
        }

        serverUrl = config.at("/clusters/0/cluster/server").asText();
        certificateAuthorityData = config.at("/clusters/0/cluster/certificate-authority-data").asText();
        clientCertificateData = config.at("/users/0/user/client-certificate-data").asText();
        clientKeyData = config.at("/users/0/user/client-key-data").asText();

        workspace = new FilePath(tmpFolder.getRoot());

        run = mock(Run.class);
        launcher = mock(Launcher.class);

        taskListener = mock(TaskListener.class);
        when(taskListener.getLogger()).thenReturn(System.err);
        doAnswer(new Answer<PrintWriter>() {
            @Override
            public PrintWriter answer(InvocationOnMock invocationOnMock) throws Throwable {
                String msg = invocationOnMock.getArgument(0);
                System.err.print(msg);
                return new PrintWriter(System.err);
            }
        }).when(taskListener).error(any(String.class));
        when(run.getDisplayName()).thenReturn("acs-test-run");
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
    }

    protected class DeploymentWatcher implements AutoCloseable {
        final String name;
        final Watch deploymentWatch;
        final CountDownLatch latch;

        public DeploymentWatcher(String namespace, String name) {
            this.name = name;
            latch = new CountDownLatch(1);
            KubernetesClient client = new KubernetesClientWrapper(workspaceKubeConfig.getAbsolutePath()).getClient();
            deploymentWatch =
                    client.extensions()
                            .deployments()
                            .inNamespace(namespace)
                            .withName(name)
                            .watch(new Watcher<Deployment>() {
                                @Override
                                public void eventReceived(Action action, Deployment deployment) {
                                    Integer availableReplica = deployment.getStatus().getAvailableReplicas();
                                    if (availableReplica != null && availableReplica > 0) {
                                        latch.countDown();
                                    }
                                }

                                @Override
                                public void onClose(KubernetesClientException e) {
                                    if (e != null) {
                                        LOGGER.log(Level.SEVERE, null, e);
                                        throw e;
                                    }
                                }
                            });
        }

        public void waitReady(long timeout, TimeUnit tu) throws InterruptedException {
            if (!latch.await(timeout, tu)) {
                Assert.fail("Timeout while waiting for the deployment " + name + " to become ready");
            }
        }

        @Override
        public void close() {
            if (deploymentWatch != null) {
                deploymentWatch.close();
            }
        }
    }
}

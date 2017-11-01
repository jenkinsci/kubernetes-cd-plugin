/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.integration;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import com.microsoft.jenkins.kubernetes.KubernetesDeploy;
import com.microsoft.jenkins.kubernetes.KubernetesDeployContext;
import com.microsoft.jenkins.kubernetes.credentials.ConfigFileCredentials;
import com.microsoft.jenkins.kubernetes.credentials.SSHCredentials;
import com.microsoft.jenkins.kubernetes.credentials.TextCredentials;
import hudson.model.Result;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.microsoft.jenkins.kubernetes.integration.TestHelpers.loadFile;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class KubernetesDeployIT extends IntegrationTest {
    private static final Logger LOGGER = Logger.getLogger(KubernetesDeployIT.class.getName());

    private KubernetesDeployContext context;

    @Before
    @Override
    public void setup() throws Exception {
        super.setup();

        context = new KubernetesDeployContext();

        context.setConfigs("k8s/*.yml");
        context.setEnableConfigSubstitution(false);
        context.setSecretNamespace("default");
    }

    @Test
    public void testSshCredentialsDeployment() throws Exception {
        loadFile(getClass(), workspace, "k8s/ssh.yml");

        String sshCredentialsId;
        BasicSSHUserPrivateKey sshCredentials = new BasicSSHUserPrivateKey(
                CredentialsScope.GLOBAL,
                sshCredentialsId = UUID.randomUUID().toString(),
                adminUser,
                new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource(privateKeyPath),
                null,
                "Kubernetes master SSH credentials");
        SystemCredentialsProvider.getInstance().getDomainCredentialsMap().get(Domain.global()).add(sshCredentials);

        context.setCredentialsType("SSH");
        SSHCredentials cred = new SSHCredentials();
        cred.setSshServer(masterHost + ":" + port);
        cred.setSshCredentialsId(sshCredentialsId);
        context.setSsh(cred);

        KubernetesClient client = new KubernetesClientWrapper(workspaceKubeConfig.getAbsolutePath()).getClient();
        final CountDownLatch serviceLatch = new CountDownLatch(1);
        Watch serviceWatcher =
                client.services()
                        .inNamespace("default")
                        .withName("ssh-credentials-test-service")
                        .watch(new Watcher<Service>() {
                            @Override
                            public void eventReceived(Action action, Service service) {
                                LoadBalancerStatus loadBalancerStatus = service.getStatus().getLoadBalancer();
                                if (loadBalancerStatus == null) {
                                    return;
                                }
                                List<LoadBalancerIngress> ingresses = loadBalancerStatus.getIngress();
                                if (ingresses.size() > 0) {
                                    String ip = ingresses.get(0).getIp();
                                    if (StringUtils.isNotBlank(ip)) {
                                        LOGGER.log(Level.INFO, "Service IP for SSH credentials based deployment: " + ip);
                                        serviceLatch.countDown();
                                    }
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

        try {
            deploy("default", "ssh-credentials-test-deployment", context);
            if (!serviceLatch.await(15, TimeUnit.MINUTES)) {
                // it generally takes more than 5 minutes for the load balancer and the front-end IP, NSG ready for
                // the service.
                Assert.fail("Timeout waiting for the service deployment to be ready");
            }
        } finally {
            serviceWatcher.close();
        }
    }

    @Test
    public void testConfigFileCredentials() throws Exception {
        loadFile(getClass(), workspace, "k8s/config-file.yml");

        withKubeConfig();

        deploy("default", "config-file-test", context);
    }

    @Test
    public void testTextCredentials() throws Exception {
        loadFile(getClass(), workspace, "k8s/text.yml");

        context.setCredentialsType("Text");
        TextCredentials cred = new TextCredentials();
        cred.setCertificateAuthorityData(certificateAuthorityData);
        cred.setClientCertificateData(clientCertificateData);
        cred.setClientKeyData(clientKeyData);
        cred.setServerUrl(serverUrl);
        context.setTextCredentials(cred);

        deploy("default", "text-test", context);
    }

    @Test
    public void testEnvironmentSubstitute() throws Exception {
        loadFile(getClass(), workspace, "k8s/substitute.yml");

        envVars.put("DEPLOYMENT_NAME", "substitute-test");
        envVars.put("IMAGE_NAME", "nginx");

        context.setEnableConfigSubstitution(true);

        withKubeConfig();

        deploy("default", "substitute-test", context);
    }

    @Test
    public void testPrivateRegistry() throws Exception {
        if (StringUtils.isBlank(dockerRepository)) {
            return;
        }
        Assert.assertTrue(StringUtils.isNotBlank(dockerUsername));
        Assert.assertTrue(StringUtils.isNotBlank(dockerPassword));

        loadFile(getClass(), workspace, "k8s/private.yml");

        String credId = UUID.randomUUID().toString();
        UsernamePasswordCredentials userPass = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                credId,
                "Docker Registry Credentials For Kubernetes CD Test",
                dockerUsername,
                dockerPassword);
        SystemCredentialsProvider.getInstance().getDomainCredentialsMap().get(Domain.global()).add(userPass);

        context.setEnableConfigSubstitution(true);
        envVars.put("KUBERNETES_CD_DOCKER_REPOSITORY", dockerRepository);
        envVars.put("IMAGE_NAME", "kubernetes-test");

        DockerRegistryEndpoint endpoint = new DockerRegistryEndpoint(dockerRegistry, credId);
        context.setDockerCredentials(Collections.singletonList(endpoint));

        withKubeConfig();

        deploy("default", "private-test", context);
    }

    @Test
    public void testOverrideExisting() throws Exception {
        loadFile(getClass(), workspace, "k8s/override1.yml", "k8s/override.yml");

        withKubeConfig();

        deploy("default", "override-test", context);

        loadFile(getClass(), workspace, "k8s/override2.yml", "k8s/override.yml");

        new KubernetesDeploy(context).perform(run, workspace, launcher, taskListener);
        verify(run, never()).setResult(Result.FAILURE);

        KubernetesClient client = new KubernetesClientWrapper(workspaceKubeConfig.getAbsolutePath()).getClient();
        Deployment deployment =
                client.extensions().deployments()
                        .inNamespace("default")
                        .withName("override-test")
                        .get();
        Assert.assertNotNull(deployment);
        Assert.assertEquals("nginx:alpine", deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
    }

    private void withKubeConfig() {
        context.setCredentialsType("KubeConfig");
        ConfigFileCredentials cred = new ConfigFileCredentials();
        cred.setPath(workspaceKubeConfig.getName());
        context.setKubeConfig(cred);
    }

    private void deploy(String namespace, String name, KubernetesDeployContext context) throws Exception {
        try (DeploymentWatcher watcher = new DeploymentWatcher(namespace, name)) {
            new KubernetesDeploy(context).perform(run, workspace, launcher, taskListener);
            verify(run, never()).setResult(Result.FAILURE);

            watcher.waitReady(5, TimeUnit.MINUTES);
        }
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.jenkins.kubernetes.util.CommonUtils;
import com.microsoft.jenkins.kubernetes.util.Constants;
import com.microsoft.jenkins.kubernetes.util.DockerConfigBuilder;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Item;
import hudson.util.VariableResolver;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class KubernetesClientWrapper {
    private final KubernetesClient client;
    private PrintStream logger;
    private VariableResolver<String> variableResolver;
    private ResourceUpdateMonitor resourceUpdateMonitor = ResourceUpdateMonitor.NOOP;

    @VisibleForTesting
    KubernetesClientWrapper(KubernetesClient client) {
        this.client = client;
    }

    public KubernetesClientWrapper(String kubeConfigFilePath) {
        client = new DefaultKubernetesClient(kubeConfigFromFile(kubeConfigFilePath));
    }

    public KubernetesClientWrapper(String server,
                                   String certificateAuthorityData,
                                   String clientCertificateData,
                                   String clientKeyData) {
        Config config = new ConfigBuilder()
                .withMasterUrl(server)
                .withCaCertData(certificateAuthorityData)
                .withClientCertData(clientCertificateData)
                .withClientKeyData(clientKeyData)
                .withWebsocketPingInterval(0)
                .build();
        client = new DefaultKubernetesClient(config);
    }

    public KubernetesClient getClient() {
        return client;
    }

    public PrintStream getLogger() {
        return logger;
    }

    public KubernetesClientWrapper withLogger(PrintStream log) {
        this.logger = log;
        return this;
    }

    public VariableResolver<String> getVariableResolver() {
        return variableResolver;
    }

    public KubernetesClientWrapper withVariableResolver(VariableResolver<String> resolver) {
        this.variableResolver = resolver;
        return this;
    }

    public ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public KubernetesClientWrapper withResourceUpdateMonitor(ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }


    /**
     * Apply Kubernetes configurations through the given Kubernetes client.
     *
     * @param namespace   The namespace that the components should be created / updated
     * @param configFiles The configuration files to be deployed
     * @throws IOException          exception on IO
     * @throws InterruptedException interruption happened during blocking IO operations
     */
    public void apply(String namespace, FilePath[] configFiles) throws IOException, InterruptedException {
        for (FilePath path : configFiles) {
            logger.println(Messages.KubernetesClientWrapper_loadingConfiguration(path));

            List<HasMetadata> resources = client.load(CommonUtils.replaceMacro(path.read(), variableResolver)).get();
            if (resources.isEmpty()) {
                logger.println(Messages.KubernetesClientWrapper_noResourceLoadedFrom(path));
                continue;
            }
            for (HasMetadata resource : resources) {
                if (resource instanceof Deployment) {
                    Deployment deployment = (Deployment) resource;

                    Deployment originalDeployment = null;
                    boolean checkDeployment = resourceUpdateMonitor.isInterestedInDeployment();
                    if (checkDeployment) {
                        originalDeployment = client
                                .extensions()
                                .deployments()
                                .inNamespace(namespace)
                                .withName(deployment.getMetadata().getName())
                                .get();
                    }

                    deployment = client
                            .extensions()
                            .deployments()
                            .inNamespace(namespace)
                            .createOrReplace(deployment);

                    if (checkDeployment) {
                        resourceUpdateMonitor.onDeploymentUpdate(originalDeployment, deployment);
                    }

                    logger.println(Messages.KubernetesClientWrapper_appliedDeployment(deployment));
                } else if (resource instanceof Service) {
                    Service service = (Service) resource;

                    Service originalService = null;
                    boolean checkService = resourceUpdateMonitor.isInterestedInService();
                    if (checkService) {
                        originalService = client
                                .services()
                                .inNamespace(namespace)
                                .withName(service.getMetadata().getName())
                                .get();
                    }

                    service = client
                            .services()
                            .inNamespace(namespace)
                            .createOrReplace(service);

                    if (checkService) {
                        resourceUpdateMonitor.onServiceUpdate(originalService, service);
                    }

                    logger.println(Messages.KubernetesClientWrapper_appliedService(service));
                } else if (resource instanceof Ingress) {
                    Ingress ingress = (Ingress) resource;

                    Ingress originalIngress = null;
                    boolean checkIngress = resourceUpdateMonitor.isInterestedInIngress();
                    if (checkIngress) {
                        originalIngress = client
                                .extensions()
                                .ingresses()
                                .inNamespace(namespace)
                                .withName(ingress.getMetadata().getName())
                                .get();
                    }

                    ingress = client
                            .extensions()
                            .ingresses()
                            .inNamespace(namespace)
                            .createOrReplace(ingress);

                    if (checkIngress) {
                        resourceUpdateMonitor.onIngressUpdate(originalIngress, ingress);
                    }

                    logger.println(Messages.KubernetesClientWrapper_appliedIngress(ingress));
                } else {
                    logger.println(Messages.KubernetesClientWrapper_skipped(resource));
                }
            }
        }
    }

    /**
     * Construct the dockercfg with all the provided credentials, and create a new Secret resource for the Kubernetes
     * cluster.
     * <p>
     * This can be used by the Pods later to pull images from the private container registry.
     *
     * @param context             the current job context, generally this should be {@code getRun().getParent()}
     * @param kubernetesNamespace The namespace that the Secret should be created / updated
     * @param secretName          The name of the Secret
     * @param credentials         All the configured credentials
     * @throws IOException exception on IO
     * @see <a href="https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry">
     * Pull an Image from a Private Registry
     * </a>
     */
    public void createOrReplaceSecrets(
            Item context,
            String kubernetesNamespace,
            String secretName,
            List<DockerRegistryEndpoint> credentials) throws IOException {
        logger.println(Messages.KubernetesClientWrapper_prepareSecretsWithName(secretName));

        DockerConfigBuilder dockerConfigBuilder = new DockerConfigBuilder(credentials);
        String dockercfg = dockerConfigBuilder.buildDockercfgBase64(context);

        Map<String, String> data = new HashMap<>();
        data.put(".dockercfg", dockercfg);
        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(kubernetesNamespace)
                .endMetadata()
                .withData(data)
                .withType("kubernetes.io/dockercfg")
                .build();

        Secret originalSecret = null;
        boolean checkSecret = resourceUpdateMonitor.isInterestedInSecret();
        if (checkSecret) {
            originalSecret = client.secrets().inNamespace(kubernetesNamespace).withName(secretName).get();
        }

        Secret updatedSecret = client.secrets().inNamespace(kubernetesNamespace).createOrReplace(secret);
        if (checkSecret) {
            resourceUpdateMonitor.onSecretUpdate(originalSecret, updatedSecret);
        }
    }

    /**
     * Build config from the kubeconfig file.
     * <p>
     * This requires to update the system property. In order to avoid changing the system property at the same time
     * from multiple running jobs, the method is marked as synchronized.
     *
     * @param filePath the kubeconfig file path
     * @return the config that can be used to build {@link KubernetesClient}
     */
    private static synchronized Config kubeConfigFromFile(String filePath) {
        String originalTryConfig = System.getProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY);
        String originalFile = System.getProperty(Config.KUBERNETES_KUBECONFIG_FILE);
        try {
            System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "true");
            System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, filePath);
            return Config.autoConfigure();
        } finally {
            if (originalFile == null) {
                System.clearProperty(Config.KUBERNETES_KUBECONFIG_FILE);
            } else {
                System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, originalFile);
            }
            if (originalTryConfig == null) {
                System.clearProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY);
            } else {
                System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, originalTryConfig);
            }
        }
    }

    public static String prepareSecretName(String nameCfg, String defaultName, EnvVars envVars) {
        String name = StringUtils.trimToEmpty(envVars.expand(nameCfg));
        if (name.length() > Constants.KUBERNETES_NAME_LENGTH_LIMIT) {
            throw new IllegalArgumentException(Messages.KubernetesClientWrapper_secretNameTooLong(name));
        }

        if (!name.isEmpty()) {
            if (!Constants.KUBERNETES_NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException(Messages.KubernetesClientWrapper_illegalSecretName(name));
            }

            return name;
        }
        // use default name and ensure it conforms the requirements.
        name = defaultName;
        if (StringUtils.isBlank(name)) {
            name = UUID.randomUUID().toString();
        }
        name = Constants.KUBERNETES_SECRET_NAME_PREFIX
                + name.replaceAll("[^0-9a-zA-Z]", "-").toLowerCase();
        if (name.length() > Constants.KUBERNETES_NAME_LENGTH_LIMIT) {
            name = name.substring(0, Constants.KUBERNETES_NAME_LENGTH_LIMIT);
        }
        if (name.charAt(name.length() - 1) == '-') {
            name = name.substring(0, name.length() - 1) + 'a';
        }
        return name;
    }
}

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
import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
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
    private PrintStream logger = System.out;
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
            log(Messages.KubernetesClientWrapper_loadingConfiguration(path));

            List<HasMetadata> resources = client.load(CommonUtils.replaceMacro(path.read(), variableResolver)).get();
            if (resources.isEmpty()) {
                log(Messages.KubernetesClientWrapper_noResourceLoadedFrom(path));
                continue;
            }
            for (HasMetadata resource : resources) {
                if (resource instanceof Deployment) {
                    Deployment deployment = (Deployment) resource;
                    new DeploymentUpdater(namespace, deployment).createOrReplace();
                } else if (resource instanceof Service) {
                    Service service = (Service) resource;
                    new ServiceUpdater(namespace, service).createOrReplace();
                } else if (resource instanceof Ingress) {
                    Ingress ingress = (Ingress) resource;
                    new IngressUpdater(namespace, ingress).createOrReplace();
                } else if (resource instanceof ReplicationController) {
                    ReplicationController replicationController = (ReplicationController) resource;
                    new ReplicationControllerUpdater(namespace, replicationController).createOrReplace();
                } else if (resource instanceof ReplicaSet) {
                    ReplicaSet rs = (ReplicaSet) resource;
                    new ReplicaSetUpdater(namespace, rs).createOrReplace();
                } else if (resource instanceof DaemonSet) {
                    DaemonSet daemonSet = (DaemonSet) resource;
                    new DaemonSetUpdater(namespace, daemonSet).createOrReplace();
                } else if (resource instanceof Job) {
                    Job job = (Job) resource;
                    new JobUpdater(namespace, job).createOrReplace();
                } else if (resource instanceof Pod) {
                    Pod pod = (Pod) resource;
                    new PodUpdator(namespace, pod).createOrReplace();
                } else if (resource instanceof Secret) {
                    Secret secret = (Secret) resource;
                    new SecretUpdater(namespace, secret).createOrReplace();
                } else {
                    log(Messages.KubernetesClientWrapper_skipped(resource));
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
        log(Messages.KubernetesClientWrapper_prepareSecretsWithName(secretName));

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
    public static synchronized Config kubeConfigFromFile(String filePath) {
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

    private void log(String message) {
        if (logger != null) {
            logger.println(message);
        }
    }

    private abstract class ResourceUpdater<T extends HasMetadata> {
        private final String namespace;
        private final T resource;

        ResourceUpdater(String namespace, T resource) {
            this.namespace = namespace;
            this.resource = resource;
        }

        String getNamespace() {
            return namespace;
        }

        T get() {
            return resource;
        }

        final void createOrReplace() {
            T original = null;
            boolean monitor = isInterestedInUpdate();
            if (monitor) {
                original = getCurrentResource(get().getMetadata().getName());
            }
            T updated = applyResource(get());
            if (monitor) {
                notifyUpdate(original, updated);
            }
            logApplied(updated);
        }

        abstract boolean isInterestedInUpdate();

        abstract T getCurrentResource(String name);

        abstract T applyResource(T res);

        abstract void notifyUpdate(T original, T current);

        void logApplied(T res) {
            log(Messages.KubernetesClientWrapper_applied(res.getClass().getSimpleName(), res));
        }
    }

    private class DeploymentUpdater extends ResourceUpdater<Deployment> {
        DeploymentUpdater(String namespace, Deployment deployment) {
            super(namespace, deployment);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInDeployment();
        }

        @Override
        Deployment getCurrentResource(String name) {
            return client
                    .extensions()
                    .deployments()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        Deployment applyResource(Deployment res) {
            return client
                    .extensions()
                    .deployments()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(Deployment original, Deployment current) {
            resourceUpdateMonitor.onDeploymentUpdate(original, current);
        }
    }

    private class ServiceUpdater extends ResourceUpdater<Service> {
        ServiceUpdater(String namespace, Service service) {
            super(namespace, service);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInService();
        }

        @Override
        Service getCurrentResource(String name) {
            return client
                    .services()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        Service applyResource(Service res) {
            return client
                    .services()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(Service original, Service current) {
            resourceUpdateMonitor.onServiceUpdate(original, current);
        }
    }

    private class IngressUpdater extends ResourceUpdater<Ingress> {
        IngressUpdater(String namespace, Ingress ingress) {
            super(namespace, ingress);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInIngress();
        }

        @Override
        Ingress getCurrentResource(String name) {
            return client
                    .extensions()
                    .ingresses()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        Ingress applyResource(Ingress res) {
            return client
                    .extensions()
                    .ingresses()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(Ingress original, Ingress current) {
            resourceUpdateMonitor.onIngressUpdate(original, current);
        }
    }

    private class ReplicationControllerUpdater extends ResourceUpdater<ReplicationController> {
        ReplicationControllerUpdater(String namespace, ReplicationController rc) {
            super(namespace, rc);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInReplicationController();
        }

        @Override
        ReplicationController getCurrentResource(String name) {
            return client
                    .replicationControllers()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        ReplicationController applyResource(ReplicationController res) {
            return client
                    .replicationControllers()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(ReplicationController original, ReplicationController current) {
            resourceUpdateMonitor.onReplicationControllerUpdate(original, current);
        }
    }

    private class ReplicaSetUpdater extends ResourceUpdater<ReplicaSet> {
        ReplicaSetUpdater(String namespace, ReplicaSet rs) {
            super(namespace, rs);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInReplicaSet();
        }

        @Override
        ReplicaSet getCurrentResource(String name) {
            return client
                    .extensions()
                    .replicaSets()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        ReplicaSet applyResource(ReplicaSet res) {
            return client
                    .extensions()
                    .replicaSets()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(ReplicaSet original, ReplicaSet current) {
            resourceUpdateMonitor.onReplicaSetUpdate(original, current);
        }
    }

    private class DaemonSetUpdater extends ResourceUpdater<DaemonSet> {
        DaemonSetUpdater(String namespace, DaemonSet ds) {
            super(namespace, ds);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInDaemonSet();
        }

        @Override
        DaemonSet getCurrentResource(String name) {
            return client
                    .extensions()
                    .daemonSets()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        DaemonSet applyResource(DaemonSet res) {
            return client
                    .extensions()
                    .daemonSets()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(DaemonSet original, DaemonSet current) {
            resourceUpdateMonitor.onDaemonSetUpdate(original, current);
        }
    }

    private class JobUpdater extends ResourceUpdater<Job> {
        JobUpdater(String namespace, Job job) {
            super(namespace, job);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInJob();
        }

        @Override
        Job getCurrentResource(String name) {
            return client
                    .extensions()
                    .jobs()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        Job applyResource(Job res) {
            return client
                    .extensions()
                    .jobs()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(Job original, Job current) {
            resourceUpdateMonitor.onJobUpdate(original, current);
        }
    }

    private class PodUpdator extends ResourceUpdater<Pod> {
        PodUpdator(String namespace, Pod pod) {
            super(namespace, pod);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInPod();
        }

        @Override
        Pod getCurrentResource(String name) {
            return client
                    .pods()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        Pod applyResource(Pod res) {
            return client
                    .pods()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(Pod original, Pod current) {
            resourceUpdateMonitor.onPodUpdate(original, current);
        }
    }

    private class SecretUpdater extends ResourceUpdater<Secret> {
        SecretUpdater(String namespace, Secret secret) {
            super(namespace, secret);
        }

        @Override
        boolean isInterestedInUpdate() {
            return resourceUpdateMonitor.isInterestedInSecret();
        }

        @Override
        Secret getCurrentResource(String name) {
            return client
                    .secrets()
                    .inNamespace(getNamespace())
                    .withName(name)
                    .get();
        }

        @Override
        Secret applyResource(Secret res) {
            return client
                    .secrets()
                    .inNamespace(getNamespace())
                    .createOrReplace(res);
        }

        @Override
        void notifyUpdate(Secret original, Secret current) {
            resourceUpdateMonitor.onSecretUpdate(original, current);
        }

        @Override
        void logApplied(Secret res) {
            // do not show the secret details
            log(Messages.KubernetesClientWrapper_applied("Secret", "name: " + res.getMetadata().getName()));
        }
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.CommonUtils;
import com.microsoft.jenkins.kubernetes.util.Constants;
import com.microsoft.jenkins.kubernetes.util.DockerConfigBuilder;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.util.VariableResolver;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.CronJob;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class KubernetesClientWrapper {
    private final KubernetesClient client;
    private PrintStream logger = System.out;
    private VariableResolver<String> variableResolver;
    private ResourceUpdateMonitor resourceUpdateMonitor = ResourceUpdateMonitor.NOOP;

    @VisibleForTesting
    KubernetesClientWrapper(KubernetesClient client) {
        this.client = client;
    }

    public KubernetesClientWrapper(String kubeconfig) {
        File file = new File(kubeconfig);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                kubeconfig = IOUtils.toString(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Config config = configFromKubeconfig(kubeconfig);
        client = new DefaultKubernetesClient(config);
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
     * @param configFiles The configuration files to be deployed
     * @throws IOException          exception on IO
     * @throws InterruptedException interruption happened during blocking IO operations
     */
    public void apply(FilePath[] configFiles) throws IOException, InterruptedException {
        for (FilePath path : configFiles) {
            log(Messages.KubernetesClientWrapper_loadingConfiguration(path));

            List<HasMetadata> resources = client.load(CommonUtils.replaceMacro(path.read(), variableResolver)).get();
            if (resources.isEmpty()) {
                log(Messages.KubernetesClientWrapper_noResourceLoadedFrom(path));
                continue;
            }

            // Process the Namespace in the list first, as it may be a dependency of other resources.
            Iterator<HasMetadata> iter = resources.iterator();
            while (iter.hasNext()) {
                HasMetadata resource = iter.next();
                if (resource instanceof Namespace) {
                    Namespace namespace = (Namespace) resource;
                    new NamespaceUpdater(namespace).createOrApply();
                    iter.remove();
                }
            }

            for (HasMetadata resource : resources) {
                if (resource instanceof Deployment) {
                    Deployment deployment = (Deployment) resource;
                    new DeploymentUpdater(deployment).createOrApply();
                } else if (resource instanceof Service) {
                    Service service = (Service) resource;
                    new ServiceUpdater(service).createOrApply();
                } else if (resource instanceof Ingress) {
                    Ingress ingress = (Ingress) resource;
                    new IngressUpdater(ingress).createOrApply();
                } else if (resource instanceof ReplicationController) {
                    ReplicationController replicationController = (ReplicationController) resource;
                    new ReplicationControllerUpdater(replicationController).createOrApply();
                } else if (resource instanceof ReplicaSet) {
                    ReplicaSet rs = (ReplicaSet) resource;
                    new ReplicaSetUpdater(rs).createOrApply();
                } else if (resource instanceof DaemonSet) {
                    DaemonSet daemonSet = (DaemonSet) resource;
                    new DaemonSetUpdater(daemonSet).createOrApply();
                } else if (resource instanceof Job) {
                    Job job = (Job) resource;
                    new JobUpdater(job).createOrApply();
                } else if (resource instanceof CronJob) {
                    CronJob cronJob = (CronJob) resource;
                    new CronJobUpdater(cronJob).createOrApply();
                } else if (resource instanceof Pod) {
                    Pod pod = (Pod) resource;
                    new PodUpdater(pod).createOrApply();
                } else if (resource instanceof Secret) {
                    Secret secret = (Secret) resource;
                    new SecretUpdater(secret).createOrApply();
                } else if (resource instanceof ConfigMap) {
                    ConfigMap configMap = (ConfigMap) resource;
                    new ConfigMapUpdater(configMap).createOrApply();
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
     * @param kubernetesNamespace The namespace in which the Secret should be created / updated
     * @param secretName          The name of the Secret
     * @param credentials         All the configured credentials
     * @throws IOException exception on IO
     * @see <a href="https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry">
     * Pull an Image from a Private Registry
     * </a>
     */
    public void createOrReplaceSecrets(
            String kubernetesNamespace,
            String secretName,
            List<ResolvedDockerRegistryEndpoint> credentials) throws IOException {
        log(Messages.KubernetesClientWrapper_prepareSecretsWithName(secretName));

        DockerConfigBuilder dockerConfigBuilder = new DockerConfigBuilder(credentials);
        String dockercfg = dockerConfigBuilder.buildDockercfgBase64();

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

        new SecretUpdater(secret).createOrApply();
    }

    /**
     * Build config from the kubeconfig contents.
     * <p>
     * This requires to update the system property. In order to avoid changing the system property at the same time
     * from multiple running jobs, the method is marked as synchronized.
     *
     * @param kubeconfig the kubeconfig contents
     * @return the config that can be used to build {@link KubernetesClient}
     */
    public static synchronized Config configFromKubeconfig(String kubeconfig) {
        String originalTryKubeconfig =
                Utils.getSystemPropertyOrEnvVar(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY);
        String originalTryServiceAccount =
                Utils.getSystemPropertyOrEnvVar(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY);
        String originalTryNamespacePath =
                Utils.getSystemPropertyOrEnvVar(Config.KUBERNETES_TRYNAMESPACE_PATH_SYSTEM_PROPERTY);
        try {
            System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
            System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");
            System.setProperty(Config.KUBERNETES_TRYNAMESPACE_PATH_SYSTEM_PROPERTY, "false");
            return Config.fromKubeconfig(kubeconfig);
        } finally {
            restoreProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, originalTryKubeconfig);
            restoreProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, originalTryServiceAccount);
            restoreProperty(Config.KUBERNETES_TRYNAMESPACE_PATH_SYSTEM_PROPERTY, originalTryNamespacePath);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
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
        int suffixLength = Constants.KUBERNETES_NAME_LENGTH_LIMIT - name.length();
        final int randomLength = 8;
        if (suffixLength > randomLength) {
            suffixLength = randomLength;
        }
        String suffix = CommonUtils.randomString(suffixLength, true);
        name += suffix;

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
        private final T resource;

        ResourceUpdater(T resource) {
            checkNotNull(resource);
            this.resource = resource;
            checkState(StringUtils.isNotBlank(getName()),
                    Messages.KubernetesClientWrapper_noName(), getKind(), resource);
        }

        final String getNamespace() {
            ObjectMeta metadata = resource.getMetadata();
            String ns = null;
            if (metadata != null) {
                ns = resource.getMetadata().getNamespace();
            }
            if (ns == null) {
                ns = Constants.DEFAULT_KUBERNETES_NAMESPACE;
            }
            return ns;
        }

        final T get() {
            return resource;
        }

        final String getName() {
            ObjectMeta metadata = resource.getMetadata();
            String name = null;
            if (metadata != null) {
                name = metadata.getName();
            }
            return name;
        }

        final String getKind() {
            return resource.getKind();
        }

        /**
         * Explicitly apply the configuration if a resource with the same name exists in the namespace in the cluster,
         * or create one if not.
         * <p>
         * If we cannot load resource during application (possibly because the resource gets deleted after we first
         * checked), or some one created the resource after we checked and before we created, the method fails with
         * exception.
         *
         * @throws IOException if we cannot find the resource in the cluster when we apply the configuration
         */
        final void createOrApply() throws IOException {
            T original = getCurrentResource();
            T current = get();
            T updated;
            if (original != null) {
                updated = applyResource(original, current);
                if (updated == null) {
                    throw new IOException(Messages.KubernetesClientWrapper_resourceNotFound(
                            getKind(), current.getMetadata().getName()));
                }
                logApplied(updated);
            } else {
                updated = createResource(get());
                logCreated(updated);
            }
            notifyUpdate(original, updated);
        }

        abstract T getCurrentResource();

        abstract T applyResource(T original, T current);

        abstract T createResource(T current);

        abstract void notifyUpdate(T original, T current);

        void logApplied(T res) {
            log(Messages.KubernetesClientWrapper_applied(res.getClass().getSimpleName(), res));
        }

        void logCreated(T res) {
            log(Messages.KubernetesClientWrapper_created(res.getClass().getSimpleName(), res));
        }
    }

    private class DeploymentUpdater extends ResourceUpdater<Deployment> {
        DeploymentUpdater(Deployment deployment) {
            super(deployment);
        }

        @Override
        Deployment getCurrentResource() {
            return client
                    .apps()
                    .deployments()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        Deployment applyResource(Deployment original, Deployment current) {
            return client
                    .apps()
                    .deployments()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        Deployment createResource(Deployment current) {
            return client
                    .apps()
                    .deployments()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(Deployment original, Deployment current) {
            resourceUpdateMonitor.onDeploymentUpdate(original, current);
        }
    }

    private class ServiceUpdater extends ResourceUpdater<Service> {
        ServiceUpdater(Service service) {
            super(service);
        }

        @Override
        Service getCurrentResource() {
            return client
                    .services()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        Service applyResource(Service original, Service current) {
            List<ServicePort> originalPorts = original.getSpec().getPorts();
            List<ServicePort> currentPorts = current.getSpec().getPorts();
            // Pin the nodePort to the public port
            // The kubernetes-client library will compare the server config and the current applied config,
            // and compute the difference, which will be sent to the PATCH API of Kubernetes. The missing nodePort
            // will be considered as deletion, which will cause the Kubernetes to assign a new nodePort to the
            // service, which may have problem with the port forwarding as in the load balancer.
            //
            // "kubectl apply" handles the service update in the same way.
            if (originalPorts != null && currentPorts != null) {
                Map<Integer, Integer> portToNodePort = new HashMap<>();
                for (ServicePort servicePort : originalPorts) {
                    Integer port = servicePort.getPort();
                    Integer nodePort = servicePort.getNodePort();
                    if (port != null && nodePort != null) {
                        portToNodePort.put(servicePort.getPort(), servicePort.getNodePort());
                    }
                }
                for (ServicePort servicePort : currentPorts) {
                    // if the nodePort is defined in the config, use it
                    Integer currentNodePort = servicePort.getNodePort();
                    if (currentNodePort != null && currentNodePort != 0) {
                        continue;
                    }
                    // otherwise try to copy the nodePort from the current service status
                    Integer port = servicePort.getPort();
                    if (port != null) {
                        Integer nodePort = portToNodePort.get(port);
                        if (nodePort != null) {
                            servicePort.setNodePort(nodePort);
                        }
                    }
                }
            }

            // this should be no-op, keep it in case current.getSpec().getPorts() behavior changes in future
            current.getSpec().setPorts(currentPorts);

            return client.services()
                    .inNamespace(getNamespace())
                    .withName(original.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        Service createResource(Service current) {
            return client
                    .services()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(Service original, Service current) {
            resourceUpdateMonitor.onServiceUpdate(original, current);
        }
    }

    private class IngressUpdater extends ResourceUpdater<Ingress> {
        IngressUpdater(Ingress ingress) {
            super(ingress);
        }

        @Override
        Ingress getCurrentResource() {
            return client
                    .extensions()
                    .ingresses()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        Ingress applyResource(Ingress original, Ingress current) {
            return client
                    .extensions()
                    .ingresses()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        Ingress createResource(Ingress current) {
            return client
                    .extensions()
                    .ingresses()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(Ingress original, Ingress current) {
            resourceUpdateMonitor.onIngressUpdate(original, current);
        }
    }

    private class ReplicationControllerUpdater extends ResourceUpdater<ReplicationController> {
        ReplicationControllerUpdater(ReplicationController rc) {
            super(rc);
        }

        @Override
        ReplicationController getCurrentResource() {
            return client
                    .replicationControllers()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        ReplicationController applyResource(ReplicationController original, ReplicationController current) {
            return client
                    .replicationControllers()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        ReplicationController createResource(ReplicationController current) {
            return client
                    .replicationControllers()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(ReplicationController original, ReplicationController current) {
            resourceUpdateMonitor.onReplicationControllerUpdate(original, current);
        }
    }

    private class ReplicaSetUpdater extends ResourceUpdater<ReplicaSet> {
        ReplicaSetUpdater(ReplicaSet rs) {
            super(rs);
        }

        @Override
        ReplicaSet getCurrentResource() {
            return client
                    .apps()
                    .replicaSets()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        ReplicaSet applyResource(ReplicaSet original, ReplicaSet current) {
            return client
                    .apps()
                    .replicaSets()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        ReplicaSet createResource(ReplicaSet current) {
            return client
                    .apps()
                    .replicaSets()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(ReplicaSet original, ReplicaSet current) {
            resourceUpdateMonitor.onReplicaSetUpdate(original, current);
        }
    }

    private class DaemonSetUpdater extends ResourceUpdater<DaemonSet> {
        DaemonSetUpdater(DaemonSet ds) {
            super(ds);
        }

        @Override
        DaemonSet getCurrentResource() {
            return client
                    .apps()
                    .daemonSets()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        DaemonSet applyResource(DaemonSet original, DaemonSet current) {
            return client
                    .apps()
                    .daemonSets()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        DaemonSet createResource(DaemonSet current) {
            return client
                    .apps()
                    .daemonSets()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(DaemonSet original, DaemonSet current) {
            resourceUpdateMonitor.onDaemonSetUpdate(original, current);
        }
    }

    private class JobUpdater extends ResourceUpdater<Job> {
        JobUpdater(Job job) {
            super(job);
        }

        @Override
        Job getCurrentResource() {
            return client
                    .batch()
                    .jobs()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        Job applyResource(Job original, Job current) {
            return client
                    .batch()
                    .jobs()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        Job createResource(Job current) {
            return client
                    .batch()
                    .jobs()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(Job original, Job current) {
            resourceUpdateMonitor.onJobUpdate(original, current);
        }
    }

    private class CronJobUpdater extends ResourceUpdater<CronJob> {
        CronJobUpdater(CronJob cronJob) {
            super(cronJob);
        }

        @Override
        CronJob getCurrentResource() {
            return client
                    .batch()
                    .cronjobs()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        CronJob applyResource(CronJob original, CronJob current) {
            return client
                    .batch()
                    .cronjobs()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        CronJob createResource(CronJob current) {
            return client
                    .batch()
                    .cronjobs()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(CronJob original, CronJob current) {
            resourceUpdateMonitor.onCronJobUpdate(original, current);
        }
    }

    private class PodUpdater extends ResourceUpdater<Pod> {
        PodUpdater(Pod pod) {
            super(pod);
        }

        @Override
        Pod getCurrentResource() {
            return client
                    .pods()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        Pod applyResource(Pod original, Pod current) {
            return client
                    .pods()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        Pod createResource(Pod current) {
            return client
                    .pods()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(Pod original, Pod current) {
            resourceUpdateMonitor.onPodUpdate(original, current);
        }
    }

    private class ConfigMapUpdater extends ResourceUpdater<ConfigMap> {
        ConfigMapUpdater(ConfigMap configMap) {
            super(configMap);
        }

        @Override
        ConfigMap getCurrentResource() {
            return client
                    .configMaps()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        ConfigMap applyResource(ConfigMap original, ConfigMap current) {
            return client
                    .configMaps()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withData(current.getData())
                    .done();
        }

        @Override
        ConfigMap createResource(ConfigMap current) {
            return client
                    .configMaps()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(ConfigMap original, ConfigMap current) {
            resourceUpdateMonitor.onConfigMapUpdate(original, current);
        }
    }

    private class SecretUpdater extends ResourceUpdater<Secret> {
        SecretUpdater(Secret secret) {
            super(secret);
        }

        @Override
        Secret getCurrentResource() {
            return client
                    .secrets()
                    .inNamespace(getNamespace())
                    .withName(getName())
                    .get();
        }

        @Override
        Secret applyResource(Secret original, Secret current) {
            return client
                    .secrets()
                    .inNamespace(getNamespace())
                    .withName(current.getMetadata().getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withData(current.getData())
                    .withStringData(current.getStringData())
                    .withType(current.getType())
                    .done();
        }

        @Override
        Secret createResource(Secret current) {
            return client
                    .secrets()
                    .inNamespace(getNamespace())
                    .create(current);
        }

        @Override
        void notifyUpdate(Secret original, Secret current) {
            resourceUpdateMonitor.onSecretUpdate(original, current);
        }

        @Override
        void logApplied(Secret res) {
            // do not show the secret details
            log(Messages.KubernetesClientWrapper_applied("Secret", "name: " + getName()));
        }

        @Override
        void logCreated(Secret res) {
            log(Messages.KubernetesClientWrapper_created(getKind(), "name: " + getName()));
        }
    }

    private class NamespaceUpdater extends ResourceUpdater<Namespace> {
        NamespaceUpdater(Namespace namespace) {
            super(namespace);
        }

        @Override
        Namespace getCurrentResource() {
            return client
                    .namespaces()
                    .withName(getName())
                    .get();
        }

        @Override
        Namespace applyResource(Namespace original, Namespace current) {
            return client
                    .namespaces()
                    .withName(getName())
                    .edit()
                    .withMetadata(current.getMetadata())
                    .withSpec(current.getSpec())
                    .done();
        }

        @Override
        Namespace createResource(Namespace current) {
            return client
                    .namespaces()
                    .create(current);
        }

        @Override
        void notifyUpdate(Namespace original, Namespace current) {
            resourceUpdateMonitor.onNamespaceUpdate(original, current);
        }
    }
}

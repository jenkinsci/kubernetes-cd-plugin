/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1DaemonSet;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1ReplicaSet;
import io.kubernetes.client.models.V1ReplicationController;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServicePort;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class V1ResourceManager extends ResourceManager {
    private CoreV1Api coreV1ApiInstance;
    private AppsV1Api appsV1ApiInstance;
    private BatchV1Api batchV1ApiInstance;
    private String pretty = DEFAULT_PRETTY;
    private V1ResourceUpdateMonitor resourceUpdateMonitor = V1ResourceUpdateMonitor.NOOP;

    public V1ResourceManager() {
        coreV1ApiInstance = new CoreV1Api();
        appsV1ApiInstance = new AppsV1Api();
        batchV1ApiInstance = new BatchV1Api();
    }

    public V1ResourceManager(String pretty) {
        this();
        this.pretty = pretty;
    }

    @Override
    public boolean apply(Object resource) throws IOException {
        if (resource instanceof V1Namespace) {
            V1Namespace namespace = (V1Namespace) resource;
            new NamespaceUpdater(namespace).createOrApply();
        } else if (resource instanceof V1Deployment) {
            V1Deployment deployment = (V1Deployment) resource;
            new DeploymentUpdater(deployment).createOrApply();
        } else if (resource instanceof V1Service) {
            V1Service service = (V1Service) resource;
            new ServiceUpdater(service).createOrApply();
        } else if (resource instanceof V1ReplicationController) {
            V1ReplicationController replicationController = (V1ReplicationController) resource;
            new ReplicationControllerUpdater(replicationController).createOrApply();
        } else if (resource instanceof V1ReplicaSet) {
            V1ReplicaSet replicaSet = (V1ReplicaSet) resource;
            new ReplicaSetUpdater(replicaSet).createOrApply();
        } else if (resource instanceof V1DaemonSet) {
            V1DaemonSet daemonSet = (V1DaemonSet) resource;
            new DaemonSetUpdater(daemonSet).createOrApply();
        } else if (resource instanceof V1Job) {
            V1Job job = (V1Job) resource;
            new JobUpdater(job).createOrApply();
        } else if (resource instanceof V1Pod) {
            V1Pod pod = (V1Pod) resource;
            new PodUpdater(pod).createOrApply();
        } else if (resource instanceof V1Secret) {
            V1Secret secret = (V1Secret) resource;
            new SecretUpdater(secret).createOrApply();
        } else if (resource instanceof V1ConfigMap) {
            V1ConfigMap configMap = (V1ConfigMap) resource;
            new ConfigMapUpdater(configMap).createOrApply();
        } else {
            return false;
        }
        return true;
    }

    public V1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V1ResourceManager withResourceUpdateMonitor(V1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    private class ReplicaSetUpdater extends ResourceUpdater<V1ReplicaSet> {
        ReplicaSetUpdater(V1ReplicaSet rs) {
            super(rs);
        }

        @Override
        V1ReplicaSet getCurrentResource() {
            V1ReplicaSet replicaSet = null;
            try {
                replicaSet = appsV1ApiInstance.readNamespacedReplicaSet(getName(), getNamespace(), pretty,
                        true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1ReplicaSet applyResource(V1ReplicaSet original, V1ReplicaSet current) {
            V1ReplicaSet replicaSet = null;
            try {
                replicaSet = appsV1ApiInstance.replaceNamespacedReplicaSet(getName(), getNamespace(), current,
                        pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1ReplicaSet createResource(V1ReplicaSet current) {
            V1ReplicaSet replicaSet = null;
            try {
                replicaSet = appsV1ApiInstance.createNamespacedReplicaSet(getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        void notifyUpdate(V1ReplicaSet original, V1ReplicaSet current) {
            resourceUpdateMonitor.onReplicaSetUpdate(original, current);
        }
    }

    private class DeploymentUpdater extends ResourceUpdater<V1Deployment> {
        DeploymentUpdater(V1Deployment deployment) {
            super(deployment);
        }

        @Override
        V1Deployment getCurrentResource() {
            V1Deployment deployment = null;
            try {
                deployment = appsV1ApiInstance.readNamespacedDeployment(getName(), getNamespace(), pretty,
                        true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        V1Deployment applyResource(V1Deployment original, V1Deployment current) {
            V1Deployment deployment = null;
            try {
                deployment = appsV1ApiInstance.replaceNamespacedDeployment(getName(), getNamespace(), current,
                        pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        V1Deployment createResource(V1Deployment current) {
            V1Deployment deployment = null;
            try {
                deployment = appsV1ApiInstance.createNamespacedDeployment(getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }


        @Override
        void notifyUpdate(V1Deployment original, V1Deployment current) {
            resourceUpdateMonitor.onDeploymentUpdate(original, current);
        }
    }


    private class DaemonSetUpdater extends ResourceUpdater<V1DaemonSet> {
        DaemonSetUpdater(V1DaemonSet ds) {
            super(ds);
        }

        @Override
        V1DaemonSet getCurrentResource() {
            V1DaemonSet daemonSet = null;
            try {
                daemonSet = appsV1ApiInstance.readNamespacedDaemonSet(getName(), getNamespace(), pretty,
                        true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1DaemonSet applyResource(V1DaemonSet original, V1DaemonSet current) {
            V1DaemonSet daemonSet = null;
            try {
                daemonSet = appsV1ApiInstance.replaceNamespacedDaemonSet(getName(), getNamespace(), current,
                        pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1DaemonSet createResource(V1DaemonSet current) {
            V1DaemonSet daemonSet = null;
            try {
                daemonSet = appsV1ApiInstance.createNamespacedDaemonSet(getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        void notifyUpdate(V1DaemonSet original, V1DaemonSet current) {
            resourceUpdateMonitor.onDaemonSetUpdate(original, current);
        }
    }

    private class ReplicationControllerUpdater extends ResourceUpdater<V1ReplicationController> {
        ReplicationControllerUpdater(V1ReplicationController rc) {
            super(rc);
        }

        @Override
        V1ReplicationController getCurrentResource() {
            V1ReplicationController replicationController = null;
            try {
                replicationController = coreV1ApiInstance.readNamespacedReplicationController(getName(), getNamespace(),
                        pretty, true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicationController;
        }

        @Override
        V1ReplicationController applyResource(V1ReplicationController original, V1ReplicationController current) {
            V1ReplicationController replicationController = null;
            try {
                replicationController = coreV1ApiInstance.replaceNamespacedReplicationController(getName(),
                        getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicationController;
        }

        @Override
        V1ReplicationController createResource(V1ReplicationController current) {
            V1ReplicationController replicationController = null;
            try {
                replicationController = coreV1ApiInstance.createNamespacedReplicationController(getNamespace(),
                        current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicationController;
        }

        @Override
        void notifyUpdate(V1ReplicationController original, V1ReplicationController current) {
            resourceUpdateMonitor.onReplicationControllerUpdate(original, current);
        }
    }


    private class ServiceUpdater extends ResourceUpdater<V1Service> {
        ServiceUpdater(V1Service service) {
            super(service);
        }

        @Override
        V1Service getCurrentResource() {
            V1Service service = null;
            try {
                service = coreV1ApiInstance.readNamespacedService(getName(), getNamespace(), pretty,
                        true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return service;
        }

        @Override
        V1Service applyResource(V1Service original, V1Service current) {
            List<V1ServicePort> originalPorts = original.getSpec().getPorts();
            List<V1ServicePort> currentPorts = current.getSpec().getPorts();
            // Pin the nodePort to the public port
            // The kubernetes-client library will compare the server config and the current applied config,
            // and compute the difference, which will be sent to the PATCH API of Kubernetes. The missing nodePort
            // will be considered as deletion, which will cause the Kubernetes to assign a new nodePort to the
            // service, which may have problem with the port forwarding as in the load balancer.
            //
            // "kubectl apply" handles the service update in the same way.
            if (originalPorts != null && currentPorts != null) {
                Map<Integer, Integer> portToNodePort = new HashMap<>();
                for (V1ServicePort servicePort : originalPorts) {
                    Integer port = servicePort.getPort();
                    Integer nodePort = servicePort.getNodePort();
                    if (port != null && nodePort != null) {
                        portToNodePort.put(servicePort.getPort(), servicePort.getNodePort());
                    }
                }
                for (V1ServicePort servicePort : currentPorts) {
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

            V1Service service = null;
            try {
                service = coreV1ApiInstance.replaceNamespacedService(getName(), getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return service;
        }

        @Override
        V1Service createResource(V1Service current) {
            V1Service service = null;
            try {
                service = coreV1ApiInstance.createNamespacedService(getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return service;
        }

        @Override
        void notifyUpdate(V1Service original, V1Service current) {
            resourceUpdateMonitor.onServiceUpdate(original, current);
        }
    }

    private class JobUpdater extends ResourceUpdater<V1Job> {
        JobUpdater(V1Job job) {
            super(job);
        }

        @Override
        V1Job getCurrentResource() {
            V1Job job = null;
            try {
                job = batchV1ApiInstance.readNamespacedJob(getName(), getNamespace(), pretty, true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return job;
        }

        @Override
        V1Job applyResource(V1Job original, V1Job current) {
            V1Job job = null;
            try {
                job = batchV1ApiInstance.replaceNamespacedJob(getName(), getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return job;
        }

        @Override
        V1Job createResource(V1Job current) {
            V1Job job = null;
            try {
                job = batchV1ApiInstance.createNamespacedJob(getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return job;
        }

        @Override
        void notifyUpdate(V1Job original, V1Job current) {
            resourceUpdateMonitor.onJobUpdate(original, current);
        }
    }

    private class PodUpdater extends ResourceUpdater<V1Pod> {
        PodUpdater(V1Pod pod) {
            super(pod);
        }

        @Override
        V1Pod getCurrentResource() {
            V1Pod pod = null;
            try {
                pod = coreV1ApiInstance.readNamespacedPod(getName(), getNamespace(), pretty, true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return pod;
        }

        @Override
        V1Pod applyResource(V1Pod original, V1Pod current) {
            V1Pod pod = null;
            try {
                pod = coreV1ApiInstance.replaceNamespacedPod(getName(), getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return pod;
        }

        @Override
        V1Pod createResource(V1Pod current) {
            V1Pod pod = null;
            try {
                pod = coreV1ApiInstance.createNamespacedPod(getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return pod;
        }

        @Override
        void notifyUpdate(V1Pod original, V1Pod current) {
            resourceUpdateMonitor.onPodUpdate(original, current);
        }
    }

    private class ConfigMapUpdater extends ResourceUpdater<V1ConfigMap> {
        ConfigMapUpdater(V1ConfigMap configMap) {
            super(configMap);
        }

        @Override
        V1ConfigMap getCurrentResource() {
            V1ConfigMap configMap = null;
            try {
                configMap = coreV1ApiInstance.readNamespacedConfigMap(getName(), getNamespace(), pretty, true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return configMap;
        }

        @Override
        V1ConfigMap applyResource(V1ConfigMap original, V1ConfigMap current) {
            V1ConfigMap configMap = null;
            try {
                configMap = coreV1ApiInstance.replaceNamespacedConfigMap(getName(), getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return configMap;
        }

        @Override
        V1ConfigMap createResource(V1ConfigMap current) {
            V1ConfigMap configMap = null;
            try {
                configMap = coreV1ApiInstance.createNamespacedConfigMap(getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return configMap;
        }

        @Override
        void notifyUpdate(V1ConfigMap original, V1ConfigMap current) {
            resourceUpdateMonitor.onConfigMapUpdate(original, current);
        }
    }

    private class SecretUpdater extends ResourceUpdater<V1Secret> {
        SecretUpdater(V1Secret secret) {
            super(secret);
        }

        @Override
        V1Secret getCurrentResource() {
            V1Secret secret = null;
            try {
                secret = coreV1ApiInstance.readNamespacedSecret(getName(), getNamespace(), pretty, true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return secret;
        }

        @Override
        V1Secret applyResource(V1Secret original, V1Secret current) {
            V1Secret secret = null;
            try {
                secret = coreV1ApiInstance.replaceNamespacedSecret(getName(), getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return secret;
        }

        @Override
        V1Secret createResource(V1Secret current) {
            V1Secret secret = null;
            try {
                secret = coreV1ApiInstance.createNamespacedSecret(getNamespace(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return secret;
        }

        @Override
        void notifyUpdate(V1Secret original, V1Secret current) {
            resourceUpdateMonitor.onSecretUpdate(original, current);
        }

        @Override
        void logApplied(V1Secret res) {
            // do not show the secret details
            getLogger().info(Messages.KubernetesClientWrapper_applied("Secret", "name: " + getName()));
        }

        @Override
        void logCreated(V1Secret res) {
            getLogger().info(Messages.KubernetesClientWrapper_created(getKind(), "name: " + getName()));
        }
    }

    private class NamespaceUpdater extends ResourceUpdater<V1Namespace> {
        NamespaceUpdater(V1Namespace namespace) {
            super(namespace);
        }

        @Override
        V1Namespace getCurrentResource() {
            V1Namespace result = null;
            try {
                result = coreV1ApiInstance.readNamespace(getName(), pretty, true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Namespace applyResource(V1Namespace original, V1Namespace current) {
            V1Namespace result = null;
            try {
                result = coreV1ApiInstance.replaceNamespace(getName(), current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Namespace createResource(V1Namespace current) {
            V1Namespace result = null;
            try {
                result = coreV1ApiInstance.createNamespace(current, pretty);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1Namespace original, V1Namespace current) {
            resourceUpdateMonitor.onNamespaceUpdate(original, current);
        }
    }
}

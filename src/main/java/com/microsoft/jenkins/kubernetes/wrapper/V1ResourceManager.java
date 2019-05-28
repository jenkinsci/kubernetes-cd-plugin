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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class V1ResourceManager extends ResourceManager {
    private static final CoreV1Api CORE_V1_API_INSTANCE = new CoreV1Api();
    private static final AppsV1Api APPS_V1_API_INSTANCE = new AppsV1Api();
    private static final BatchV1Api BATCH_V1_API_INSTANCE = new BatchV1Api();
    private V1ResourceUpdateMonitor resourceUpdateMonitor = V1ResourceUpdateMonitor.NOOP;

    public V1ResourceManager() {
        super(true);
    }

    public V1ResourceManager(boolean pretty) {
        super(pretty);
    }

    public V1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V1ResourceManager withResourceUpdateMonitor(V1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    class ReplicaSetUpdater extends ResourceUpdater<V1ReplicaSet> {
        ReplicaSetUpdater(V1ReplicaSet rs) {
            super(rs);
        }

        @Override
        V1ReplicaSet getCurrentResource() {
            V1ReplicaSet replicaSet = null;
            try {
                replicaSet = APPS_V1_API_INSTANCE.readNamespacedReplicaSet(getName(), getNamespace(), getPretty(),
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
                replicaSet = APPS_V1_API_INSTANCE.replaceNamespacedReplicaSet(getName(), getNamespace(), current,
                        getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1ReplicaSet createResource(V1ReplicaSet current) {
            V1ReplicaSet replicaSet = null;
            try {
                replicaSet = APPS_V1_API_INSTANCE.createNamespacedReplicaSet(getNamespace(), current,null, getPretty(),null);
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

    class DeploymentUpdater extends ResourceUpdater<V1Deployment> {
        DeploymentUpdater(V1Deployment deployment) {
            super(deployment);
        }

        @Override
        V1Deployment getCurrentResource() {
            V1Deployment deployment = null;
            try {
                deployment = APPS_V1_API_INSTANCE.readNamespacedDeployment(getName(), getNamespace(), getPretty(),
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
                deployment = APPS_V1_API_INSTANCE.replaceNamespacedDeployment(getName(), getNamespace(), current,
                        getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        V1Deployment createResource(V1Deployment current) {
            V1Deployment deployment = null;
            try {
                deployment = APPS_V1_API_INSTANCE.createNamespacedDeployment(getNamespace(), current,null, getPretty(),null);
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


    class DaemonSetUpdater extends ResourceUpdater<V1DaemonSet> {
        DaemonSetUpdater(V1DaemonSet ds) {
            super(ds);
        }

        @Override
        V1DaemonSet getCurrentResource() {
            V1DaemonSet daemonSet = null;
            try {
                daemonSet = APPS_V1_API_INSTANCE.readNamespacedDaemonSet(getName(), getNamespace(), getPretty(),
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
                daemonSet = APPS_V1_API_INSTANCE.replaceNamespacedDaemonSet(getName(), getNamespace(), current,
                        getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1DaemonSet createResource(V1DaemonSet current) {
            V1DaemonSet daemonSet = null;
            try {
                daemonSet = APPS_V1_API_INSTANCE.createNamespacedDaemonSet(getNamespace(), current,null ,getPretty(),null);
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

    class ReplicationControllerUpdater extends ResourceUpdater<V1ReplicationController> {
        ReplicationControllerUpdater(V1ReplicationController rc) {
            super(rc);
        }

        @Override
        V1ReplicationController getCurrentResource() {
            V1ReplicationController replicationController = null;
            try {
                replicationController = CORE_V1_API_INSTANCE.readNamespacedReplicationController(getName(),
                        getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicationController;
        }

        @Override
        V1ReplicationController applyResource(V1ReplicationController original, V1ReplicationController current) {
            V1ReplicationController replicationController = null;
            try {
                replicationController = CORE_V1_API_INSTANCE.replaceNamespacedReplicationController(getName(),
                        getNamespace(), current, getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicationController;
        }

        @Override
        V1ReplicationController createResource(V1ReplicationController current) {
            V1ReplicationController replicationController = null;
            try {
                replicationController = CORE_V1_API_INSTANCE.createNamespacedReplicationController(getNamespace(),
                        current,null, getPretty(),null);
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


    class ServiceUpdater extends ResourceUpdater<V1Service> {
        ServiceUpdater(V1Service service) {
            super(service);
        }

        @Override
        V1Service getCurrentResource() {
            V1Service service = null;
            try {
                service = CORE_V1_API_INSTANCE.readNamespacedService(getName(), getNamespace(), getPretty(),
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
                service = CORE_V1_API_INSTANCE.replaceNamespacedService(getName(), getNamespace(),
                        current, getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return service;
        }

        @Override
        V1Service createResource(V1Service current) {
            V1Service service = null;
            try {
                service = CORE_V1_API_INSTANCE.createNamespacedService(getNamespace(), current,null, getPretty(),null);
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

    class JobUpdater extends ResourceUpdater<V1Job> {
        JobUpdater(V1Job job) {
            super(job);
        }

        @Override
        V1Job getCurrentResource() {
            V1Job job = null;
            try {
                job = BATCH_V1_API_INSTANCE.readNamespacedJob(getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return job;
        }

        @Override
        V1Job applyResource(V1Job original, V1Job current) {
            V1Job job = null;
            try {
                job = BATCH_V1_API_INSTANCE.replaceNamespacedJob(getName(), getNamespace(), current, getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return job;
        }

        @Override
        V1Job createResource(V1Job current) {
            V1Job job = null;
            try {
                job = BATCH_V1_API_INSTANCE.createNamespacedJob(getNamespace(), current,null, getPretty(),null);
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

    class PodUpdater extends ResourceUpdater<V1Pod> {
        PodUpdater(V1Pod pod) {
            super(pod);
        }

        @Override
        V1Pod getCurrentResource() {
            V1Pod pod = null;
            try {
                pod = CORE_V1_API_INSTANCE.readNamespacedPod(getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return pod;
        }

        @Override
        V1Pod applyResource(V1Pod original, V1Pod current) {
            V1Pod pod = null;
            try {
                pod = CORE_V1_API_INSTANCE.replaceNamespacedPod(getName(), getNamespace(), current, getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return pod;
        }

        @Override
        V1Pod createResource(V1Pod current) {
            V1Pod pod = null;
            try {
                pod = CORE_V1_API_INSTANCE.createNamespacedPod(getNamespace(), current,null, getPretty(),null);
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

    class ConfigMapUpdater extends ResourceUpdater<V1ConfigMap> {
        ConfigMapUpdater(V1ConfigMap configMap) {
            super(configMap);
        }

        @Override
        V1ConfigMap getCurrentResource() {
            V1ConfigMap configMap = null;
            try {
                configMap = CORE_V1_API_INSTANCE.readNamespacedConfigMap(getName(), getNamespace(),
                        getPretty(), true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return configMap;
        }

        @Override
        V1ConfigMap applyResource(V1ConfigMap original, V1ConfigMap current) {
            V1ConfigMap configMap = null;
            try {
                configMap = CORE_V1_API_INSTANCE.replaceNamespacedConfigMap(getName(), getNamespace(),
                        current, getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return configMap;
        }

        @Override
        V1ConfigMap createResource(V1ConfigMap current) {
            V1ConfigMap configMap = null;
            try {
                configMap = CORE_V1_API_INSTANCE.createNamespacedConfigMap(getNamespace(), current,null, getPretty(),null);
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

    class SecretUpdater extends ResourceUpdater<V1Secret> {
        SecretUpdater(V1Secret secret) {
            super(secret);
        }

        @Override
        V1Secret getCurrentResource() {
            V1Secret secret = null;
            try {
                secret = CORE_V1_API_INSTANCE.readNamespacedSecret(getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return secret;
        }

        @Override
        V1Secret applyResource(V1Secret original, V1Secret current) {
            V1Secret secret = null;
            try {
                secret = CORE_V1_API_INSTANCE.replaceNamespacedSecret(getName(), getNamespace(), current, getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return secret;
        }

        @Override
        V1Secret createResource(V1Secret current) {
            V1Secret secret = null;
            try {
                secret = CORE_V1_API_INSTANCE.createNamespacedSecret(getNamespace(), current,null, getPretty(),null);
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

    class NamespaceUpdater extends ResourceUpdater<V1Namespace> {
        NamespaceUpdater(V1Namespace namespace) {
            super(namespace);
        }

        @Override
        V1Namespace getCurrentResource() {
            V1Namespace result = null;
            try {
                result = CORE_V1_API_INSTANCE.readNamespace(getName(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Namespace applyResource(V1Namespace original, V1Namespace current) {
            V1Namespace result = null;
            try {
                result = CORE_V1_API_INSTANCE.replaceNamespace(getName(), current, getPretty(),null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Namespace createResource(V1Namespace current) {
            V1Namespace result = null;
            try {
                result = CORE_V1_API_INSTANCE.createNamespace(current,null, getPretty(),null);
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

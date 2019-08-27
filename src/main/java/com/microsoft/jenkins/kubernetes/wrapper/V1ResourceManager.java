/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import com.microsoft.jenkins.kubernetes.util.Constants;
import com.microsoft.jenkins.kubernetes.util.KubernetesJsonUtils;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.AutoscalingV1Api;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.NetworkingV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1DaemonSet;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1NetworkPolicy;
import io.kubernetes.client.models.V1PersistentVolume;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1ReplicaSet;
import io.kubernetes.client.models.V1ReplicationController;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServicePort;
import io.kubernetes.client.models.V1StatefulSet;
import io.kubernetes.client.models.V1Status;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.microsoft.jenkins.kubernetes.util.Constants.DRY_RUN_ALL;
import static com.microsoft.jenkins.kubernetes.util.Constants.KUBERNETES_CONTROLLER_UID_FIELD;
import static com.microsoft.jenkins.kubernetes.util.Constants.KUBERNETES_JOB_NAME_FIELD;

public class V1ResourceManager extends ResourceManager {

    private final CoreV1Api coreV1ApiInstance;
    private final AppsV1Api appsV1ApiInstance;
    private final BatchV1Api batchV1ApiInstance;
    private final AutoscalingV1Api autoscalingV1Api;
    private final NetworkingV1Api networkingV1Api;

    private V1ResourceUpdateMonitor resourceUpdateMonitor = V1ResourceUpdateMonitor.NOOP;

    public V1ResourceManager(ApiClient client) {
        super(true);
        checkNotNull(client);

        coreV1ApiInstance = new CoreV1Api(client);
        appsV1ApiInstance = new AppsV1Api(client);
        batchV1ApiInstance = new BatchV1Api(client);
        autoscalingV1Api = new AutoscalingV1Api(client);
        networkingV1Api = new NetworkingV1Api(client);
    }

    public V1ResourceManager(ApiClient client, boolean pretty) {
        super(pretty);
        checkNotNull(client);

        coreV1ApiInstance = new CoreV1Api(client);
        appsV1ApiInstance = new AppsV1Api(client);
        batchV1ApiInstance = new BatchV1Api(client);
        autoscalingV1Api = new AutoscalingV1Api(client);
        networkingV1Api = new NetworkingV1Api(client);

    }

    /**
     * In the case of different image names, the default imagePullPolicy will be different.
     * E.g. Nginx => Always   Nginx:1.79 => IfNotPresent
     * This method is used to mask the default value caused by dryRun.
     *
     * @param origin object in kubernetes cluster
     * @param yaml   user applied object
     * @param target dryRun return object
     */
    private static void recoverPodImagePullPolicy(V1PodSpec origin, V1PodSpec yaml, V1PodSpec target) {
        if (origin.getInitContainers() != null) {
            checkNotNull(yaml.getInitContainers());
            checkNotNull(target.getInitContainers());
            // should not change size
            if (origin.getInitContainers().size() != yaml.getInitContainers().size()
                    || origin.getInitContainers().size() != target.getInitContainers().size()) {
                return;
            }
            for (int i = 0; i < origin.getInitContainers().size(); i++) {
                if (StringUtils.isBlank(yaml.getInitContainers().get(i).getImagePullPolicy())) {
                    target.getInitContainers().get(i).imagePullPolicy(
                            origin.getInitContainers().get(i).getImagePullPolicy());
                }
            }
        }
        if (origin.getContainers() != null) {
            checkNotNull(yaml.getContainers());
            checkNotNull(target.getContainers());
            // should not change size
            if (origin.getContainers().size() != yaml.getContainers().size()
                    || origin.getContainers().size() != target.getContainers().size()) {
                return;
            }
            for (int i = 0; i < origin.getContainers().size(); i++) {
                if (StringUtils.isBlank(yaml.getContainers().get(i).getImagePullPolicy())) {
                    target.getContainers().get(i).imagePullPolicy(
                            origin.getContainers().get(i).getImagePullPolicy());
                }
            }
        }
        return;
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
                replicaSet = appsV1ApiInstance.readNamespacedReplicaSet(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return replicaSet;
        }

        @Override
        V1ReplicaSet applyResource(V1ReplicaSet original, V1ReplicaSet current) {
            V1ReplicaSet replicaSet = null;
            try {
                replicaSet = appsV1ApiInstance.replaceNamespacedReplicaSet(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1ReplicaSet createResource(V1ReplicaSet current) {
            V1ReplicaSet replicaSet = null;
            try {
                replicaSet = appsV1ApiInstance.createNamespacedReplicaSet(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1Status deleteResource(V1ReplicaSet current) {
            V1Status result = null;
            try {
                result = appsV1ApiInstance.deleteNamespacedReplicaSet(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
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
                deployment = appsV1ApiInstance.readNamespacedDeployment(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return deployment;
        }

        @Override
        V1Deployment applyResource(V1Deployment original, V1Deployment current) {
            V1Deployment deployment = null;
            try {
                deployment = appsV1ApiInstance.replaceNamespacedDeployment(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        V1Deployment createResource(V1Deployment current) {
            V1Deployment deployment = null;
            try {
                deployment = appsV1ApiInstance.createNamespacedDeployment(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        V1Status deleteResource(V1Deployment current) {
            V1Status result = null;
            try {
                result = appsV1ApiInstance.deleteNamespacedDeployment(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
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
                daemonSet = appsV1ApiInstance.readNamespacedDaemonSet(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return daemonSet;
        }

        @Override
        V1DaemonSet applyResource(V1DaemonSet original, V1DaemonSet current) {
            V1DaemonSet daemonSet = null;
            try {
                daemonSet = appsV1ApiInstance.replaceNamespacedDaemonSet(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1DaemonSet createResource(V1DaemonSet current) {
            V1DaemonSet daemonSet = null;
            try {
                daemonSet = appsV1ApiInstance.createNamespacedDaemonSet(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1Status deleteResource(V1DaemonSet current) {
            V1Status result = null;
            try {
                result = appsV1ApiInstance.deleteNamespacedDaemonSet(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
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
                replicationController = coreV1ApiInstance.readNamespacedReplicationController(getName(),
                        getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return replicationController;
        }

        @Override
        V1ReplicationController applyResource(V1ReplicationController original, V1ReplicationController current) {
            V1ReplicationController replicationController = null;
            try {
                replicationController = coreV1ApiInstance.replaceNamespacedReplicationController(getName(),
                        getNamespace(), current, getPretty(), null);
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
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicationController;
        }

        @Override
        V1Status deleteResource(V1ReplicationController current) {
            V1Status result = null;
            try {
                result = coreV1ApiInstance.deleteNamespacedReplicationController(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
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
                service = coreV1ApiInstance.readNamespacedService(getName(), getNamespace(), getPretty(),
                        true, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
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

            // update svc need to set resourceVersion
            current.getMetadata().setResourceVersion(original.getMetadata().getResourceVersion());

            // clusterIP is immutable, if the clusterIP is empty, it is set to the original clusterIP
            if (StringUtils.isBlank(current.getSpec().getClusterIP())) {
                current.getSpec().setClusterIP(original.getSpec().getClusterIP());
            }

            V1Service service = null;
            try {
                service = coreV1ApiInstance.replaceNamespacedService(getName(), getNamespace(),
                        current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return service;
        }

        @Override
        V1Service createResource(V1Service current) {
            V1Service service = null;
            try {
                service = coreV1ApiInstance.createNamespacedService(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return service;
        }

        @Override
        V1Status deleteResource(V1Service current) {
            V1Status result = null;
            try {
                result = coreV1ApiInstance.deleteNamespacedService(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
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
                job = batchV1ApiInstance.readNamespacedJob(getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return job;
        }

        @Override
        V1Job applyResource(V1Job original, V1Job current) {
            V1Job job = null;
            V1Job putJob = getPutObject(original, current);
            try {
                job = batchV1ApiInstance.replaceNamespacedJob(
                        getName(), getNamespace(), putJob, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return job;
        }

        /**
         * Get PutObject with server-dryRun.
         * Only some of the fields in the job resource can be modified.
         * Directly putting the original object of the user will cause some immutable
         * fields with defaultValue to be modified.
         * This method is a workaround that populates the default value with the server dryRun implementation.
         * https://kubernetes.io/blog/2019/01/14/apiserver-dry-run-and-kubectl-diff/
         *
         * @param original Current Object in Cluster
         * @param current Current object submitted by the user
         * @return Job With Default Value
         */
        V1Job getPutObject(V1Job original, V1Job current) {
            // Clone Object to avoid modifications to the original object.
            V1Job dryRunReq = KubernetesJsonUtils.getKubernetesJson().deserialize(
                    KubernetesJsonUtils.getKubernetesJson().serialize(current), V1Job.class);
            // Build dryRun Request Object
            V1Job dryRunRes = null;
            dryRunReq.getMetadata().setName(null);
            dryRunReq.getMetadata().setGenerateName(getName());
            dryRunReq.getMetadata().setNamespace(getNamespace());
            try {
                dryRunRes = batchV1ApiInstance.createNamespacedJob(dryRunReq.getMetadata().getNamespace(),
                        dryRunReq, null, getPretty(), DRY_RUN_ALL);
            } catch (ApiException e) {
                handleApiException(e);
            }
            checkNotNull(dryRunRes);
            // Recover metadata
            dryRunRes.getMetadata().
                    name(getName()).
                    creationTimestamp(original.getMetadata().getCreationTimestamp()).
                    selfLink(original.getMetadata().getSelfLink()).
                    uid(original.getMetadata().getUid()).
                    resourceVersion(original.getMetadata().getUid()).
                    ownerReferences(original.getMetadata().getOwnerReferences()).
                    generateName(null);

            // Recover label/selector
            // According to
            // https://github.com/kubernetes/kubernetes/blob/152b09ac550d50deeeff7162093332b4f7f0397d/pkg/registry/batch/job/strategy.go#L101
            // https://kubernetes.io/docs/concepts/workloads/controllers/jobs-run-to-completion/#specifying-your-own-pod-selector
            if (BooleanUtils.isNotTrue(current.getSpec().isManualSelector())) {
                String controllerUid = original.getMetadata().getLabels().get(KUBERNETES_CONTROLLER_UID_FIELD);
                dryRunRes.getMetadata().getLabels().put(KUBERNETES_CONTROLLER_UID_FIELD, controllerUid);
                dryRunRes.getMetadata().getLabels().put(KUBERNETES_JOB_NAME_FIELD, getName());

                dryRunRes.getSpec().getSelector().putMatchLabelsItem(KUBERNETES_CONTROLLER_UID_FIELD, controllerUid);
                dryRunRes.getSpec().getTemplate().getMetadata().getLabels().put(
                        KUBERNETES_CONTROLLER_UID_FIELD, controllerUid);
                dryRunRes.getSpec().getTemplate().getMetadata().getLabels().put(KUBERNETES_JOB_NAME_FIELD, getName());
            }

            V1ResourceManager.recoverPodImagePullPolicy(original.getSpec().getTemplate().getSpec(),
                    current.getSpec().getTemplate().getSpec(),
                    dryRunRes.getSpec().getTemplate().getSpec());
            return dryRunRes;
        }

        @Override
        V1Job createResource(V1Job current) {
            V1Job job = null;
            try {
                job = batchV1ApiInstance.createNamespacedJob(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return job;
        }

        @Override
        V1Status deleteResource(V1Job current) {
            V1Status result = null;
            try {
                result = batchV1ApiInstance.deleteNamespacedJob(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
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
                pod = coreV1ApiInstance.readNamespacedPod(getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return pod;
        }

        @Override
        V1Pod applyResource(V1Pod original, V1Pod current) {
            V1Pod pod = null;
            V1Pod putPod = getPutObject(original, current);
            try {
                pod = coreV1ApiInstance.replaceNamespacedPod(
                        getName(), getNamespace(), putPod, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return pod;
        }

        /**
         * Get PutObject with server-dryRun.
         * Only some of the fields in the job resource can be modified.
         * Directly putting the original object of the user will cause some immutable
         * fields with defaultValue to be modified.
         * This method is a workaround that populates the default value with the server dryRun implementation.
         * https://kubernetes.io/blog/2019/01/14/apiserver-dry-run-and-kubectl-diff/
         *
         * @param original Current Object in Cluster
         * @param current Current object submitted by the user
         * @return Pod With Default Value
         */
        V1Pod getPutObject(V1Pod original, V1Pod current) {
            // Clone Object to avoid modifications to the original object.
            V1Pod dryRunReq = KubernetesJsonUtils.getKubernetesJson().deserialize(
                    KubernetesJsonUtils.getKubernetesJson().serialize(current), V1Pod.class);
            // Build dryRun Request Object
            V1Pod dryRunRes = null;
            dryRunReq.getMetadata().setName(null);
            dryRunReq.getMetadata().setGenerateName(getName());
            dryRunReq.getMetadata().setNamespace(getNamespace());
            try {
                dryRunRes = coreV1ApiInstance.createNamespacedPod(dryRunReq.getMetadata().getNamespace(),
                        dryRunReq, null, getPretty(), DRY_RUN_ALL);
            } catch (ApiException e) {
                handleApiException(e);
            }

            checkNotNull(dryRunRes);
            // Recover metadata
            dryRunRes.getMetadata().
                    name(getName()).
                    creationTimestamp(original.getMetadata().getCreationTimestamp()).
                    selfLink(original.getMetadata().getSelfLink()).
                    uid(original.getMetadata().getUid()).
                    resourceVersion(original.getMetadata().getUid()).
                    ownerReferences(original.getMetadata().getOwnerReferences()).
                    generateName(null);

            // Recover node
            // According to https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#nodename
            dryRunRes.getSpec().
                    nodeName(original.getSpec().getNodeName());

            V1ResourceManager.recoverPodImagePullPolicy(
                    original.getSpec(),
                    current.getSpec(),
                    dryRunRes.getSpec());

            return dryRunRes;
        }

        @Override
        V1Pod createResource(V1Pod current) {
            V1Pod pod = null;
            try {
                pod = coreV1ApiInstance.createNamespacedPod(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return pod;
        }

        @Override
        V1Status deleteResource(V1Pod current) {
            V1Status result = null;
            try {
                result = coreV1ApiInstance.deleteNamespacedPod(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
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
                configMap = coreV1ApiInstance.readNamespacedConfigMap(getName(), getNamespace(),
                        getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return configMap;
        }

        @Override
        V1ConfigMap applyResource(V1ConfigMap original, V1ConfigMap current) {
            V1ConfigMap configMap = null;
            try {
                configMap = coreV1ApiInstance.replaceNamespacedConfigMap(getName(), getNamespace(),
                        current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return configMap;
        }

        @Override
        V1ConfigMap createResource(V1ConfigMap current) {
            V1ConfigMap configMap = null;
            try {
                configMap = coreV1ApiInstance.createNamespacedConfigMap(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return configMap;
        }

        @Override
        V1Status deleteResource(V1ConfigMap current) {
            V1Status result = null;
            try {
                result = coreV1ApiInstance.deleteNamespacedConfigMap(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
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
                secret = coreV1ApiInstance.readNamespacedSecret(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return secret;
        }

        @Override
        V1Secret applyResource(V1Secret original, V1Secret current) {
            V1Secret secret = null;
            try {
                secret = coreV1ApiInstance.replaceNamespacedSecret(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return secret;
        }

        @Override
        V1Secret createResource(V1Secret current) {
            V1Secret secret = null;
            try {
                secret = coreV1ApiInstance.createNamespacedSecret(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return secret;
        }

        @Override
        V1Status deleteResource(V1Secret current) {
            V1Status result = null;
            try {
                result = coreV1ApiInstance.deleteNamespacedSecret(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1Secret original, V1Secret current) {
            resourceUpdateMonitor.onSecretUpdate(original, current);
        }

        @Override
        void logApplied(V1Secret res) {
            // do not show the secret details
            getConsoleLogger().println(Messages.KubernetesClientWrapper_applied("Secret", "name: " + getName()));
        }

        @Override
        void logCreated(V1Secret res) {
            getConsoleLogger().println(Messages.KubernetesClientWrapper_created(getKind(), "name: " + getName()));
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
                result = coreV1ApiInstance.readNamespace(getName(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V1Namespace applyResource(V1Namespace original, V1Namespace current) {
            V1Namespace result = null;
            try {
                result = coreV1ApiInstance.replaceNamespace(getName(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Namespace createResource(V1Namespace current) {
            V1Namespace result = null;
            try {
                result = coreV1ApiInstance.createNamespace(current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V1Namespace current) {
            V1Status result = null;
            try {
                result = coreV1ApiInstance.deleteNamespace(
                        getName(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1Namespace original, V1Namespace current) {
            resourceUpdateMonitor.onNamespaceUpdate(original, current);
        }
    }

    class HorizontalPodAutoscalerUpdater extends ResourceUpdater<V1HorizontalPodAutoscaler> {
        HorizontalPodAutoscalerUpdater(V1HorizontalPodAutoscaler namespace) {
            super(namespace);
        }

        @Override
        V1HorizontalPodAutoscaler getCurrentResource() {
            V1HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV1Api.readNamespacedHorizontalPodAutoscaler(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V1HorizontalPodAutoscaler applyResource(V1HorizontalPodAutoscaler original, V1HorizontalPodAutoscaler current) {
            V1HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV1Api.replaceNamespacedHorizontalPodAutoscaler(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1HorizontalPodAutoscaler createResource(V1HorizontalPodAutoscaler current) {
            V1HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV1Api.createNamespacedHorizontalPodAutoscaler(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V1HorizontalPodAutoscaler current) {
            V1Status result = null;
            try {
                result = autoscalingV1Api.deleteNamespacedHorizontalPodAutoscaler(
                        getName(), getNamespace(), getPretty(),
                        null, null, null, null, Constants.BACKGROUND_DELETEION);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1HorizontalPodAutoscaler original, V1HorizontalPodAutoscaler current) {
            resourceUpdateMonitor.onHorizontalPodAutoscalerUpdate(original, current);
        }
    }

    class StatefulSetUpdater extends ResourceUpdater<V1StatefulSet> {
        StatefulSetUpdater(V1StatefulSet namespace) {
            super(namespace);
        }

        @Override
        V1StatefulSet getCurrentResource() {
            V1StatefulSet result = null;
            try {
                result = appsV1ApiInstance.readNamespacedStatefulSet(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V1StatefulSet applyResource(V1StatefulSet original, V1StatefulSet current) {
            V1StatefulSet result = null;
            try {
                V1StatefulSet putStatefulSet = getPutObject(original, current);
                result = appsV1ApiInstance.replaceNamespacedStatefulSet(
                        getName(), getNamespace(), putStatefulSet, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1StatefulSet createResource(V1StatefulSet current) {
            V1StatefulSet result = null;
            try {
                result = appsV1ApiInstance.createNamespacedStatefulSet(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V1StatefulSet current) {
            V1Status result = null;
            try {
                result = appsV1ApiInstance.deleteNamespacedStatefulSet(
                        getName(), getNamespace(), getPretty(),
                        null, null, null, null, Constants.BACKGROUND_DELETEION);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        /**
         * Get PutObject with server-dryRun.
         * Only some of the fields in the job resource can be modified.
         * Directly putting the original object of the user will cause some immutable
         * fields with defaultValue to be modified.
         * This method is a workaround that populates the default value with the server dryRun implementation.
         * https://kubernetes.io/blog/2019/01/14/apiserver-dry-run-and-kubectl-diff/
         *
         * @param original Current Object in Cluster
         * @param current Current object submitted by the user
         * @return StatefulSet With Default Value
         */
        V1StatefulSet getPutObject(V1StatefulSet original, V1StatefulSet current) {
            // Clone Object to avoid modifications to the original object.
            V1StatefulSet dryRunReq = KubernetesJsonUtils.getKubernetesJson().deserialize(
                    KubernetesJsonUtils.getKubernetesJson().serialize(current), V1StatefulSet.class);
            // Build dryRun Request Object
            V1StatefulSet dryRunRes = null;
            dryRunReq.getMetadata().setName(null);
            dryRunReq.getMetadata().setGenerateName(getName());
            dryRunReq.getMetadata().setNamespace(getNamespace());
            try {
                dryRunRes = appsV1ApiInstance.createNamespacedStatefulSet(dryRunReq.getMetadata().getNamespace(),
                        dryRunReq, null, getPretty(), DRY_RUN_ALL);
            } catch (ApiException e) {
                handleApiException(e);
            }
            checkNotNull(dryRunRes);
            // Recover metadata
            dryRunRes.getMetadata().
                    name(getName()).
                    creationTimestamp(original.getMetadata().getCreationTimestamp()).
                    selfLink(original.getMetadata().getSelfLink()).
                    uid(original.getMetadata().getUid()).
                    resourceVersion(original.getMetadata().getUid()).
                    ownerReferences(original.getMetadata().getOwnerReferences()).
                    generateName(null);


            V1ResourceManager.recoverPodImagePullPolicy(
                    original.getSpec().getTemplate().getSpec(),
                    current.getSpec().getTemplate().getSpec(),
                    dryRunRes.getSpec().getTemplate().getSpec());

            return dryRunRes;
        }


        @Override
        void notifyUpdate(V1StatefulSet original, V1StatefulSet current) {
            resourceUpdateMonitor.onStatefulSetUpdate(original, current);
        }
    }

    class PersistentVolumeClaimUpdater extends ResourceUpdater<V1PersistentVolumeClaim> {
        PersistentVolumeClaimUpdater(V1PersistentVolumeClaim namespace) {
            super(namespace);
        }

        @Override
        V1PersistentVolumeClaim getCurrentResource() {
            V1PersistentVolumeClaim result = null;
            try {
                result = coreV1ApiInstance.readNamespacedPersistentVolumeClaim(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V1PersistentVolumeClaim applyResource(V1PersistentVolumeClaim original, V1PersistentVolumeClaim current) {
            V1PersistentVolumeClaim result = null;
            try {
                result = coreV1ApiInstance.replaceNamespacedPersistentVolumeClaim(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1PersistentVolumeClaim createResource(V1PersistentVolumeClaim current) {
            V1PersistentVolumeClaim result = null;
            try {
                result = coreV1ApiInstance.createNamespacedPersistentVolumeClaim(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V1PersistentVolumeClaim current) {
            V1Status result = null;
            try {
                result = coreV1ApiInstance.deleteNamespacedPersistentVolumeClaim(
                        getName(), getNamespace(), getPretty(),
                        null, null, null, null, Constants.BACKGROUND_DELETEION);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1PersistentVolumeClaim original, V1PersistentVolumeClaim current) {
            resourceUpdateMonitor.onPersistentVolumeClaimUpdate(original, current);
        }
    }

    class PersistentVolumeUpdater extends ResourceUpdater<V1PersistentVolume> {
        PersistentVolumeUpdater(V1PersistentVolume persistentVolume) {
            super(persistentVolume);
        }

        @Override
        V1PersistentVolume getCurrentResource() {
            V1PersistentVolume result = null;
            try {
                result = coreV1ApiInstance.readPersistentVolume(
                        getName(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V1PersistentVolume applyResource(V1PersistentVolume original, V1PersistentVolume current) {
            V1PersistentVolume result = null;
            try {
                result = coreV1ApiInstance.replacePersistentVolume(
                        getName(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1PersistentVolume createResource(V1PersistentVolume current) {
            V1PersistentVolume result = null;
            try {
                result = coreV1ApiInstance.createPersistentVolume(
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V1PersistentVolume current) {
            V1Status result = null;
            try {
                result = coreV1ApiInstance.deletePersistentVolume(
                        getName(), getPretty(),
                        null, null, null, null, Constants.BACKGROUND_DELETEION);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1PersistentVolume original, V1PersistentVolume current) {
            resourceUpdateMonitor.onPersistentVolumeUpdate(original, current);
        }
    }

    class NetworkPolicyUpdater extends ResourceUpdater<V1NetworkPolicy> {
        NetworkPolicyUpdater(V1NetworkPolicy networkPolicy) {
            super(networkPolicy);
        }

        @Override
        V1NetworkPolicy getCurrentResource() {
            V1NetworkPolicy result = null;
            try {
                result = networkingV1Api.readNamespacedNetworkPolicy(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V1NetworkPolicy applyResource(V1NetworkPolicy original, V1NetworkPolicy current) {
            V1NetworkPolicy result = null;
            try {
                result = networkingV1Api.replaceNamespacedNetworkPolicy(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1NetworkPolicy createResource(V1NetworkPolicy current) {
            V1NetworkPolicy result = null;
            try {
                result = networkingV1Api.createNamespacedNetworkPolicy(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V1NetworkPolicy current) {
            V1Status result = null;
            try {
                result = networkingV1Api.deleteNamespacedNetworkPolicy(
                        getName(), getNamespace(), getPretty(),
                        null, null, null, null, Constants.BACKGROUND_DELETEION);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1NetworkPolicy original, V1NetworkPolicy current) {
            resourceUpdateMonitor.onNetworkPolicyUpdate(original, current);
        }
    }
}

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1beta2Api;
import io.kubernetes.client.models.V1beta2DaemonSet;
import io.kubernetes.client.models.V1beta2Deployment;

import static com.google.common.base.Preconditions.checkNotNull;

public class V1beta2ResourceManager extends ResourceManager {
    private final AppsV1beta2Api appsV1beta2Api;
    private final AppsV1beta2Api appsV1beta2PatchApi;
    private V1beta2ResourceUpdateMonitor resourceUpdateMonitor = V1beta2ResourceUpdateMonitor.NOOP;

    public V1beta2ResourceManager(ApiClient client, ApiClient strategicPatchClient) {
        super(true);
        checkNotNull(client);
        checkNotNull(strategicPatchClient);
        appsV1beta2Api = new AppsV1beta2Api(client);
        appsV1beta2PatchApi = new AppsV1beta2Api(strategicPatchClient);
    }

    public V1beta2ResourceManager(ApiClient client, ApiClient strategicPatchClient, boolean pretty) {
        super(pretty);
        checkNotNull(client);
        checkNotNull(strategicPatchClient);
        appsV1beta2Api = new AppsV1beta2Api(client);
        appsV1beta2PatchApi = new AppsV1beta2Api(strategicPatchClient);
    }

    public V1beta2ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V1beta2ResourceManager withResourceUpdateMonitor(V1beta2ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }


    class DeploymentUpdater extends ResourceUpdater<V1beta2Deployment> {
        DeploymentUpdater(V1beta2Deployment deployment) {
            super(deployment);
        }

        @Override
        V1beta2Deployment getCurrentResource() {
            V1beta2Deployment deployment = null;
            try {
                deployment = appsV1beta2Api.readNamespacedDeployment(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return deployment;
        }

        @Override
        V1beta2Deployment applyResource(V1beta2Deployment original, V1beta2Deployment current) {
            V1beta2Deployment deployment = null;
            try {
                deployment = appsV1beta2PatchApi.patchNamespacedDeployment(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        V1beta2Deployment createResource(V1beta2Deployment current) {
            V1beta2Deployment deployment = null;
            try {
                deployment = appsV1beta2Api.createNamespacedDeployment(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }


        @Override
        void notifyUpdate(V1beta2Deployment original, V1beta2Deployment current) {
            resourceUpdateMonitor.onDeploymentUpdate(original, current);
        }
    }
    class DaemonSetUpdater extends ResourceUpdater<V1beta2DaemonSet> {
        DaemonSetUpdater(V1beta2DaemonSet daemonSet) {
            super(daemonSet);
        }

        @Override
        V1beta2DaemonSet getCurrentResource() {
            V1beta2DaemonSet daemonSet = null;
            try {
                daemonSet = appsV1beta2Api.readNamespacedDaemonSet(getName(), getNamespace(),
                        getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return daemonSet;
        }

        @Override
        V1beta2DaemonSet applyResource(V1beta2DaemonSet original, V1beta2DaemonSet current) {
            V1beta2DaemonSet daemonSet = null;
            try {
                daemonSet = appsV1beta2PatchApi.patchNamespacedDaemonSet(getName(), getNamespace(),
                        current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1beta2DaemonSet createResource(V1beta2DaemonSet current) {
            V1beta2DaemonSet daemonSet = null;
            try {
                daemonSet = appsV1beta2Api.createNamespacedDaemonSet(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        void notifyUpdate(V1beta2DaemonSet original, V1beta2DaemonSet current) {
            resourceUpdateMonitor.onDaemonSetUpdate(original, current);
        }
    }
}



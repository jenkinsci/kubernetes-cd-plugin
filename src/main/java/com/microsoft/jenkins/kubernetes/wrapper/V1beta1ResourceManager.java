/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.V1beta1DaemonSet;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta1ReplicaSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class V1beta1ResourceManager extends ResourceManager {
    private final ExtensionsV1beta1Api extensionsV1beta1Api;
    private final ExtensionsV1beta1Api extensionsV1beta1PatchApi;
    private V1beta1ResourceUpdateMonitor resourceUpdateMonitor = V1beta1ResourceUpdateMonitor.NOOP;

    public V1beta1ResourceManager(ApiClient client, ApiClient strategicPatchClient) {
        super(true);
        checkNotNull(client);
        checkNotNull(strategicPatchClient);
        extensionsV1beta1Api = new ExtensionsV1beta1Api(client);
        extensionsV1beta1PatchApi = new ExtensionsV1beta1Api(strategicPatchClient);
    }

    public V1beta1ResourceManager(ApiClient client, ApiClient strategicPatchClient, boolean pretty) {
        super(pretty);
        checkNotNull(client);
        checkNotNull(strategicPatchClient);
        extensionsV1beta1Api = new ExtensionsV1beta1Api(client);
        extensionsV1beta1PatchApi = new ExtensionsV1beta1Api(strategicPatchClient);
    }

    public V1beta1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V1beta1ResourceManager withResourceUpdateMonitor(V1beta1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    class ReplicaSetUpdater extends ResourceUpdater<V1beta1ReplicaSet> {
        ReplicaSetUpdater(V1beta1ReplicaSet replicaSet) {
            super(replicaSet);
        }

        @Override
        V1beta1ReplicaSet getCurrentResource() {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1Api.readNamespacedReplicaSet(getName(), getNamespace(),
                        getPretty(), true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1beta1ReplicaSet applyResource(V1beta1ReplicaSet original, V1beta1ReplicaSet current) {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1PatchApi.patchNamespacedReplicaSet(getName(), getNamespace(),
                        current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1beta1ReplicaSet createResource(V1beta1ReplicaSet current) {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1Api.createNamespacedReplicaSet(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        void notifyUpdate(V1beta1ReplicaSet original, V1beta1ReplicaSet current) {
            resourceUpdateMonitor.onReplicaSetUpdate(original, current);
        }
    }

    class DaemonSetUpdater extends ResourceUpdater<V1beta1DaemonSet> {
        DaemonSetUpdater(V1beta1DaemonSet daemonSet) {
            super(daemonSet);
        }

        @Override
        V1beta1DaemonSet getCurrentResource() {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = extensionsV1beta1Api.readNamespacedDaemonSet(getName(), getNamespace(),
                        getPretty(), true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1beta1DaemonSet applyResource(V1beta1DaemonSet original, V1beta1DaemonSet current) {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = extensionsV1beta1PatchApi.patchNamespacedDaemonSet(getName(), getNamespace(),
                        current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1beta1DaemonSet createResource(V1beta1DaemonSet current) {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = extensionsV1beta1Api.createNamespacedDaemonSet(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        void notifyUpdate(V1beta1DaemonSet original, V1beta1DaemonSet current) {
            resourceUpdateMonitor.onDaemonSetUpdate(original, current);
        }
    }


    class IngressUpdater extends ResourceUpdater<V1beta1Ingress> {
        IngressUpdater(V1beta1Ingress ingress) {
            super(ingress);
        }

        @Override
        V1beta1Ingress getCurrentResource() {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1Api.readNamespacedIngress(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return ingress;
        }

        @Override
        V1beta1Ingress applyResource(V1beta1Ingress original, V1beta1Ingress current) {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1PatchApi.patchNamespacedIngress(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return ingress;
        }

        @Override
        V1beta1Ingress createResource(V1beta1Ingress current) {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1Api.createNamespacedIngress(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return ingress;
        }

        @Override
        void notifyUpdate(V1beta1Ingress original, V1beta1Ingress current) {
            resourceUpdateMonitor.onIngressUpdate(original, current);
        }
    }

    class DeploymentUpdater extends ResourceUpdater<ExtensionsV1beta1Deployment> {
        DeploymentUpdater(ExtensionsV1beta1Deployment ingress) {
            super(ingress);
        }

        @Override
        ExtensionsV1beta1Deployment getCurrentResource() {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1Api.readNamespacedDeployment(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        ExtensionsV1beta1Deployment applyResource(ExtensionsV1beta1Deployment original,
                                                  ExtensionsV1beta1Deployment current) {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1PatchApi.patchNamespacedDeployment(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        ExtensionsV1beta1Deployment createResource(ExtensionsV1beta1Deployment current) {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1Api.createNamespacedDeployment(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        void notifyUpdate(ExtensionsV1beta1Deployment original, ExtensionsV1beta1Deployment current) {
            resourceUpdateMonitor.onDeploymentUpdate(original, current);
        }
    }
}

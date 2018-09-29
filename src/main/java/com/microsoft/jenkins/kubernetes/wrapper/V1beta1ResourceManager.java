/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.V1beta1DaemonSet;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta1ReplicaSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class V1beta1ResourceManager extends ResourceManager {
    private static final ExtensionsV1beta1Api EXTENSIONS_V1_BETA1_API_INSTANCE = new ExtensionsV1beta1Api();
    private V1beta1ResourceUpdateMonitor resourceUpdateMonitor = V1beta1ResourceUpdateMonitor.NOOP;

    public V1beta1ResourceManager() {
        super(true);
    }

    public V1beta1ResourceManager(boolean pretty) {
        super(pretty);
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
                replicaSet = EXTENSIONS_V1_BETA1_API_INSTANCE.readNamespacedReplicaSet(getName(), getNamespace(),
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
                replicaSet = EXTENSIONS_V1_BETA1_API_INSTANCE.replaceNamespacedReplicaSet(getName(), getNamespace(),
                        current, getPretty());
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1beta1ReplicaSet createResource(V1beta1ReplicaSet current) {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = EXTENSIONS_V1_BETA1_API_INSTANCE.createNamespacedReplicaSet(getNamespace(),
                        current, getPretty());
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
                daemonSet = EXTENSIONS_V1_BETA1_API_INSTANCE.readNamespacedDaemonSet(getName(), getNamespace(),
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
                daemonSet = EXTENSIONS_V1_BETA1_API_INSTANCE.replaceNamespacedDaemonSet(getName(), getNamespace(),
                        current, getPretty());
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1beta1DaemonSet createResource(V1beta1DaemonSet current) {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = EXTENSIONS_V1_BETA1_API_INSTANCE.createNamespacedDaemonSet(getNamespace(),
                        current, getPretty());
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
                ingress = EXTENSIONS_V1_BETA1_API_INSTANCE.readNamespacedIngress(getName(), getNamespace(), getPretty(),
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
                ingress = EXTENSIONS_V1_BETA1_API_INSTANCE.replaceNamespacedIngress(getName(), getNamespace(), current,
                        getPretty());
            } catch (ApiException e) {
                handleApiException(e);
            }
            return ingress;
        }

        @Override
        V1beta1Ingress createResource(V1beta1Ingress current) {
            V1beta1Ingress ingress = null;
            try {
                ingress = EXTENSIONS_V1_BETA1_API_INSTANCE.createNamespacedIngress(getNamespace(),
                        current, getPretty());
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
}

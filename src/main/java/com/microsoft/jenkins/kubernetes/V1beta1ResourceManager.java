/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.V1beta1DaemonSet;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta1ReplicaSet;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class V1beta1ResourceManager extends ResourceManager {
    private ExtensionsV1beta1Api extensionsV1beta1ApiInstance;
    private String pretty = DEFAULT_PRETTY;
    private V1beta1ResourceUpdateMonitor resourceUpdateMonitor = V1beta1ResourceUpdateMonitor.NOOP;

    public V1beta1ResourceManager() {
        extensionsV1beta1ApiInstance = new ExtensionsV1beta1Api();
    }

    public V1beta1ResourceManager(String pretty) {
        this();
        this.pretty = pretty;
    }

    @Override
    public boolean apply(Object resource) throws IOException {
        if (resource instanceof V1beta1Ingress) {
            V1beta1Ingress ingress = (V1beta1Ingress) resource;
            new IngressUpdater(ingress).createOrApply();
        } else if (resource instanceof V1beta1DaemonSet) {
            V1beta1DaemonSet daemonSet = (V1beta1DaemonSet) resource;
            new DaemonSetUpdater(daemonSet).createOrApply();
        } else if (resource instanceof V1beta1ReplicaSet) {
            V1beta1ReplicaSet replicaSet = (V1beta1ReplicaSet) resource;
            new ReplicaSetUpdater(replicaSet).createOrApply();
        } else {
            return false;
        }
        return true;
    }

    public V1beta1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V1beta1ResourceManager withResourceUpdateMonitor(V1beta1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    private class ReplicaSetUpdater extends ResourceUpdater<V1beta1ReplicaSet> {
        ReplicaSetUpdater(V1beta1ReplicaSet replicaSet) {
            super(replicaSet);
        }

        @Override
        V1beta1ReplicaSet getCurrentResource() {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1ApiInstance.readNamespacedReplicaSet(getName(), getNamespace(), pretty, true, true);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return replicaSet;
        }

        @Override
        V1beta1ReplicaSet applyResource(V1beta1ReplicaSet original, V1beta1ReplicaSet current) {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1ApiInstance.replaceNamespacedReplicaSet(getName(), getNamespace(), current, pretty);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return replicaSet;
        }

        @Override
        V1beta1ReplicaSet createResource(V1beta1ReplicaSet current) {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1ApiInstance.createNamespacedReplicaSet(getNamespace(), current, pretty);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return replicaSet;
        }

        @Override
        void notifyUpdate(V1beta1ReplicaSet original, V1beta1ReplicaSet current) {
            resourceUpdateMonitor.onReplicaSetUpdate(original, current);
        }
    }

    private class DaemonSetUpdater extends ResourceUpdater<V1beta1DaemonSet> {
        DaemonSetUpdater(V1beta1DaemonSet daemonSet) {
            super(daemonSet);
        }

        @Override
        V1beta1DaemonSet getCurrentResource() {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = extensionsV1beta1ApiInstance.readNamespacedDaemonSet(getName(), getNamespace(), pretty, true, true);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return daemonSet;
        }

        @Override
        V1beta1DaemonSet applyResource(V1beta1DaemonSet original, V1beta1DaemonSet current) {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = extensionsV1beta1ApiInstance.replaceNamespacedDaemonSet(getName(), getNamespace(), current, pretty);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return daemonSet;
        }

        @Override
        V1beta1DaemonSet createResource(V1beta1DaemonSet current) {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = extensionsV1beta1ApiInstance.createNamespacedDaemonSet(getNamespace(), current, pretty);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return daemonSet;
        }

        @Override
        void notifyUpdate(V1beta1DaemonSet original, V1beta1DaemonSet current) {
            resourceUpdateMonitor.onDaemonSetUpdate(original, current);
        }
    }


    private class IngressUpdater extends ResourceUpdater<V1beta1Ingress> {
        IngressUpdater(V1beta1Ingress ingress) {
            super(ingress);
        }

        @Override
        V1beta1Ingress getCurrentResource() {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1ApiInstance.readNamespacedIngress(getName(), getNamespace(), pretty,
                        true, true);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return ingress;
        }

        @Override
        V1beta1Ingress applyResource(V1beta1Ingress original, V1beta1Ingress current) {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1ApiInstance.replaceNamespacedIngress(getName(), getNamespace(), current,
                        pretty);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return ingress;
        }

        @Override
        V1beta1Ingress createResource(V1beta1Ingress current) {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1ApiInstance.createNamespacedIngress(getNamespace(), current, pretty);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return ingress;
        }

        @Override
        void notifyUpdate(V1beta1Ingress original, V1beta1Ingress current) {
            resourceUpdateMonitor.onIngressUpdate(original, current);
        }
    }
}

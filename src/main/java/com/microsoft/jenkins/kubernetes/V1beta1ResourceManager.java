/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.V1beta1Ingress;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class V1beta1ResourceManager extends ResourceManager {
    private ExtensionsV1beta1Api extensionsV1beta1ApiInstance;
    private V1beta1ResourceUpdateMonitor resourceUpdateMonitor = V1beta1ResourceUpdateMonitor.NOOP;

    public V1beta1ResourceManager() {
        extensionsV1beta1ApiInstance = new ExtensionsV1beta1Api();
    }

    @Override
    public boolean apply(Object resource) throws IOException {
        if (resource instanceof V1beta1Ingress) {
            V1beta1Ingress ingress = (V1beta1Ingress) resource;
            new IngressUpdater(ingress).createOrApply();
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

    private class IngressUpdater extends ResourceUpdater<V1beta1Ingress> {
        IngressUpdater(V1beta1Ingress ingress) {
            super(ingress);
        }

        @Override
        V1beta1Ingress getCurrentResource() {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1ApiInstance.readNamespacedIngress(getName(), getNamespace(), null,
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
                        null);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return ingress;
        }

        @Override
        V1beta1Ingress createResource(V1beta1Ingress current) {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1ApiInstance.createNamespacedIngress(getNamespace(), current, null);
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

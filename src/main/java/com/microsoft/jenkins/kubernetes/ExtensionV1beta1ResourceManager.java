/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class ExtensionV1beta1ResourceManager extends ResourceManager {
    private ExtensionsV1beta1Api extensionsV1beta1ApiInstance;
    private String pretty = DEFAULT_PRETTY;
    private ExtensionV1beta1ResourceUpdateMonitor resourceUpdateMonitor = ExtensionV1beta1ResourceUpdateMonitor.NOOP;

    public ExtensionV1beta1ResourceManager() {
        extensionsV1beta1ApiInstance = new ExtensionsV1beta1Api();
    }

    public ExtensionV1beta1ResourceManager(String pretty) {
        this();
        this.pretty = pretty;
    }

    public ExtensionV1beta1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public ExtensionV1beta1ResourceManager withResourceUpdateMonitor(ExtensionV1beta1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    @Override
    public boolean apply(Object resource) throws IOException {
        if (resource instanceof ExtensionsV1beta1Deployment) {
            ExtensionsV1beta1Deployment deployment = (ExtensionsV1beta1Deployment) resource;
        } else {
            return false;
        }
        return true;
    }

    private class DeploymentUpdater extends ResourceUpdater<ExtensionsV1beta1Deployment> {
        DeploymentUpdater(ExtensionsV1beta1Deployment deployment) {
            super(deployment);
        }

        @Override
        ExtensionsV1beta1Deployment getCurrentResource() {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1ApiInstance.readNamespacedDeployment(getName(), getNamespace(), pretty,
                        true, true);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return deployment;
        }

        @Override
        ExtensionsV1beta1Deployment applyResource(ExtensionsV1beta1Deployment original,
                                                  ExtensionsV1beta1Deployment current) {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1ApiInstance.replaceNamespacedDeployment(getName(), getNamespace(),
                        current, pretty);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return deployment;
        }

        @Override
        ExtensionsV1beta1Deployment createResource(ExtensionsV1beta1Deployment current) {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1ApiInstance.createNamespacedDeployment(getNamespace(), current, pretty);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return deployment;
        }

        @Override
        void notifyUpdate(ExtensionsV1beta1Deployment original, ExtensionsV1beta1Deployment current) {
            resourceUpdateMonitor.onDeploymentUpdate(original, current);
        }
    }
}

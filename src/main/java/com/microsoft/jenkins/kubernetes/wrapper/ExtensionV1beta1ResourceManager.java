/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;

import static com.google.common.base.Preconditions.checkNotNull;

public class ExtensionV1beta1ResourceManager extends ResourceManager {
    private static final ExtensionsV1beta1Api EXTENSIONS_V1_BETA1_API_INSTANCE = new ExtensionsV1beta1Api();
    private ExtensionV1beta1ResourceUpdateMonitor resourceUpdateMonitor = ExtensionV1beta1ResourceUpdateMonitor.NOOP;

    public ExtensionV1beta1ResourceManager() {
        super(true);
    }

    public ExtensionV1beta1ResourceManager(boolean pretty) {
        super(pretty);
    }

    public ExtensionV1beta1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public ExtensionV1beta1ResourceManager withResourceUpdateMonitor(ExtensionV1beta1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    class DeploymentUpdater extends ResourceUpdater<ExtensionsV1beta1Deployment> {
        DeploymentUpdater(ExtensionsV1beta1Deployment deployment) {
            super(deployment);
        }

        @Override
        ExtensionsV1beta1Deployment getCurrentResource() {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = EXTENSIONS_V1_BETA1_API_INSTANCE.readNamespacedDeployment(getName(), getNamespace(),
                        getPretty(), true, true);
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
                deployment = EXTENSIONS_V1_BETA1_API_INSTANCE.replaceNamespacedDeployment(getName(), getNamespace(),
                        current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        ExtensionsV1beta1Deployment createResource(ExtensionsV1beta1Deployment current) {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = EXTENSIONS_V1_BETA1_API_INSTANCE.createNamespacedDeployment(getNamespace(),
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

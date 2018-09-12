/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import io.kubernetes.client.models.ExtensionsV1beta1Deployment;

public interface ExtensionV1beta1ResourceUpdateMonitor {
    ExtensionV1beta1ResourceUpdateMonitor NOOP = new Adapter();

    void onDeploymentUpdate(ExtensionsV1beta1Deployment original, ExtensionsV1beta1Deployment current);

    class Adapter implements ExtensionV1beta1ResourceUpdateMonitor {
        @Override
        public void onDeploymentUpdate(ExtensionsV1beta1Deployment original, ExtensionsV1beta1Deployment current) {
        }
    }
}

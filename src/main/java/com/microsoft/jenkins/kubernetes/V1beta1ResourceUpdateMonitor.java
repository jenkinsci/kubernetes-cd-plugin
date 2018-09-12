/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import io.kubernetes.client.models.V1beta1Ingress;

public interface V1beta1ResourceUpdateMonitor {

    void onIngressUpdate(V1beta1Ingress original, V1beta1Ingress current);

    V1beta1ResourceUpdateMonitor NOOP = new Adapter();

    class Adapter implements V1beta1ResourceUpdateMonitor {
        @Override
        public void onIngressUpdate(V1beta1Ingress original, V1beta1Ingress current) {
        }
    }
}

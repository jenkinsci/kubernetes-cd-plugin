/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.models.AppsV1beta1Deployment;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.V1beta1DaemonSet;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta1ReplicaSet;
import io.kubernetes.client.models.V1beta1StatefulSet;

public interface V1beta1ResourceUpdateMonitor {

    V1beta1ResourceUpdateMonitor NOOP = new Adapter();

    void onIngressUpdate(V1beta1Ingress original, V1beta1Ingress current);

    void onDaemonSetUpdate(V1beta1DaemonSet original, V1beta1DaemonSet current);

    void onReplicaSetUpdate(V1beta1ReplicaSet original, V1beta1ReplicaSet current);

    void onDeploymentUpdate(ExtensionsV1beta1Deployment original, ExtensionsV1beta1Deployment current);

    void onDeploymentUpdate(AppsV1beta1Deployment original, AppsV1beta1Deployment current);

    void onStatefulSetUpdate(V1beta1StatefulSet original, V1beta1StatefulSet current);

    class Adapter implements V1beta1ResourceUpdateMonitor {
        @Override
        public void onIngressUpdate(V1beta1Ingress original, V1beta1Ingress current) {
        }

        @Override
        public void onDaemonSetUpdate(V1beta1DaemonSet original, V1beta1DaemonSet current) {
        }

        @Override
        public void onReplicaSetUpdate(V1beta1ReplicaSet original, V1beta1ReplicaSet current) {
        }

        @Override
        public void onDeploymentUpdate(ExtensionsV1beta1Deployment original, ExtensionsV1beta1Deployment current) {
        }

        @Override
        public void onDeploymentUpdate(AppsV1beta1Deployment original, AppsV1beta1Deployment current) {
        }

        @Override
        public void onStatefulSetUpdate(V1beta1StatefulSet original, V1beta1StatefulSet current) {
        }
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1DaemonSet;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1ReplicaSet;
import io.kubernetes.client.models.V1ReplicationController;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1Service;

public interface V1ResourceUpdateMonitor {
    void onSecretUpdate(V1Secret original, V1Secret current);

    void onDeploymentUpdate(V1Deployment original, V1Deployment current);

    void onServiceUpdate(V1Service original, V1Service current);

    void onReplicationControllerUpdate(V1ReplicationController original, V1ReplicationController current);

    void onReplicaSetUpdate(V1ReplicaSet original, V1ReplicaSet current);

    void onDaemonSetUpdate(V1DaemonSet original, V1DaemonSet current);

    void onJobUpdate(V1Job original, V1Job current);

    void onPodUpdate(V1Pod original, V1Pod current);

    void onConfigMapUpdate(V1ConfigMap original, V1ConfigMap current);

    void onNamespaceUpdate(V1Namespace original, V1Namespace current);

    V1ResourceUpdateMonitor NOOP = new Adapter();

    class Adapter implements V1ResourceUpdateMonitor {

        @Override
        public void onSecretUpdate(V1Secret original, V1Secret current) {
        }

        @Override
        public void onDeploymentUpdate(V1Deployment original, V1Deployment current) {
        }

        @Override
        public void onServiceUpdate(V1Service original, V1Service current) {
        }

        @Override
        public void onReplicationControllerUpdate(V1ReplicationController original, V1ReplicationController current) {
        }

        @Override
        public void onReplicaSetUpdate(V1ReplicaSet original, V1ReplicaSet current) {
        }

        @Override
        public void onDaemonSetUpdate(V1DaemonSet original, V1DaemonSet current) {
        }

        @Override
        public void onJobUpdate(V1Job original, V1Job current) {
        }

        @Override
        public void onPodUpdate(V1Pod original, V1Pod current) {
        }

        @Override
        public void onConfigMapUpdate(V1ConfigMap original, V1ConfigMap current) {
        }

        @Override
        public void onNamespaceUpdate(V1Namespace original, V1Namespace current) {
        }
    }
}

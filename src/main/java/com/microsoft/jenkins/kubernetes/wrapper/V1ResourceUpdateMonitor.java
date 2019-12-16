/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicationController;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;

public interface V1ResourceUpdateMonitor {
    V1ResourceUpdateMonitor NOOP = new Adapter();

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

    void onHorizontalPodAutoscalerUpdate(V1HorizontalPodAutoscaler original, V1HorizontalPodAutoscaler current);

    void onPersistentVolumeClaimUpdate(V1PersistentVolumeClaim original, V1PersistentVolumeClaim current);

    void onPersistentVolumeUpdate(V1PersistentVolume original, V1PersistentVolume current);

    void onStatefulSetUpdate(V1StatefulSet original, V1StatefulSet current);

    void onNetworkPolicyUpdate(V1NetworkPolicy original, V1NetworkPolicy current);


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

        @Override
        public void onHorizontalPodAutoscalerUpdate(
                V1HorizontalPodAutoscaler original, V1HorizontalPodAutoscaler current) {
        }

        @Override
        public void onStatefulSetUpdate(V1StatefulSet original, V1StatefulSet current) {
        }

        @Override
        public void onPersistentVolumeClaimUpdate(
                V1PersistentVolumeClaim original, V1PersistentVolumeClaim current) {
        }

        @Override
        public void onPersistentVolumeUpdate(V1PersistentVolume original, V1PersistentVolume current) {
        }

        @Override
        public void onNetworkPolicyUpdate(V1NetworkPolicy original, V1NetworkPolicy current) {
        }
    }
}

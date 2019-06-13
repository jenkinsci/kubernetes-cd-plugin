/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.batch.CronJob;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;

public interface ResourceUpdateMonitor {
    void onSecretUpdate(Secret original, Secret current);

    void onDeploymentUpdate(Deployment original, Deployment current);

    void onStatefulSetUpdate(StatefulSet original, StatefulSet current);

    void onServiceUpdate(Service original, Service current);

    void onIngressUpdate(Ingress original, Ingress current);

    void onReplicationControllerUpdate(ReplicationController original, ReplicationController current);

    void onReplicaSetUpdate(ReplicaSet original, ReplicaSet current);

    void onDaemonSetUpdate(DaemonSet original, DaemonSet current);

    void onJobUpdate(Job original, Job current);

    void onCronJobUpdate(CronJob original, CronJob current);

    void onPodUpdate(Pod original, Pod current);

    void onConfigMapUpdate(ConfigMap original, ConfigMap current);

    void onHoriziontalPodAutoscalerUpdate(HorizontalPodAutoscaler original, HorizontalPodAutoscaler current);

    void onNamespaceUpdate(Namespace original, Namespace current);

    ResourceUpdateMonitor NOOP = new Adapter();

    class Adapter implements ResourceUpdateMonitor {

        @Override
        public void onSecretUpdate(Secret original, Secret current) {
        }

        @Override
        public void onDeploymentUpdate(Deployment original, Deployment current) {
        }

        @Override
        public void onStatefulSetUpdate(StatefulSet original, StatefulSet current) {
        }

        @Override
        public void onServiceUpdate(Service original, Service current) {
        }

        @Override
        public void onIngressUpdate(Ingress original, Ingress current) {
        }

        @Override
        public void onReplicationControllerUpdate(ReplicationController original, ReplicationController current) {
        }

        @Override
        public void onReplicaSetUpdate(ReplicaSet original, ReplicaSet current) {
        }

        @Override
        public void onDaemonSetUpdate(DaemonSet original, DaemonSet current) {
        }

        @Override
        public void onJobUpdate(Job original, Job current) {
        }

        @Override
        public void onCronJobUpdate(CronJob original, CronJob current) {
        }

        @Override
        public void onPodUpdate(Pod original, Pod current) {
        }

        @Override
        public void onHoriziontalPodAutoscalerUpdate(HorizontalPodAutoscaler original,
                                                     HorizontalPodAutoscaler current) {
        }

        @Override
        public void onConfigMapUpdate(ConfigMap original, ConfigMap current) {
        }

        @Override
        public void onNamespaceUpdate(Namespace original, Namespace current) {
        }
    }
}

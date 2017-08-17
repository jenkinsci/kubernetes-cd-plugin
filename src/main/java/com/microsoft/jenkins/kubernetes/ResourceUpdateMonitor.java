/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;

public interface ResourceUpdateMonitor {
    boolean isInterestedInSecret();

    void onSecretUpdate(Secret original, Secret current);

    boolean isInterestedInDeployment();

    void onDeploymentUpdate(Deployment original, Deployment current);

    boolean isInterestedInService();

    void onServiceUpdate(Service original, Service current);

    boolean isInterestedInIngress();

    void onIngressUpdate(Ingress original, Ingress current);

    boolean isInterestedInReplicationController();

    void onReplicationControllerUpdate(ReplicationController original, ReplicationController current);

    boolean isInterestedInReplicaSet();

    void onReplicaSetUpdate(ReplicaSet original, ReplicaSet current);

    boolean isInterestedInDaemonSet();

    void onDaemonSetUpdate(DaemonSet original, DaemonSet current);

    boolean isInterestedInJob();

    void onJobUpdate(Job original, Job current);

    boolean isInterestedInPod();

    void onPodUpdate(Pod original, Pod current);

    ResourceUpdateMonitor NOOP = new Adapter();

    class Adapter implements ResourceUpdateMonitor {
        @Override
        public boolean isInterestedInSecret() {
            return false;
        }

        @Override
        public void onSecretUpdate(Secret original, Secret current) {
        }

        @Override
        public boolean isInterestedInDeployment() {
            return false;
        }

        @Override
        public void onDeploymentUpdate(Deployment original, Deployment current) {
        }

        @Override
        public boolean isInterestedInService() {
            return false;
        }

        @Override
        public void onServiceUpdate(Service original, Service current) {
        }

        @Override
        public boolean isInterestedInIngress() {
            return false;
        }

        @Override
        public void onIngressUpdate(Ingress original, Ingress current) {
        }

        @Override
        public boolean isInterestedInReplicationController() {
            return false;
        }

        @Override
        public void onReplicationControllerUpdate(ReplicationController original, ReplicationController current) {
        }

        @Override
        public boolean isInterestedInReplicaSet() {
            return false;
        }

        @Override
        public void onReplicaSetUpdate(ReplicaSet original, ReplicaSet current) {
        }

        @Override
        public boolean isInterestedInDaemonSet() {
            return false;
        }

        @Override
        public void onDaemonSetUpdate(DaemonSet original, DaemonSet current) {
        }

        @Override
        public boolean isInterestedInJob() {
            return false;
        }

        @Override
        public void onJobUpdate(Job original, Job current) {
        }

        @Override
        public boolean isInterestedInPod() {
            return false;
        }

        @Override
        public void onPodUpdate(Pod original, Pod current) {
        }
    }
}

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.models.V1beta2DaemonSet;
import io.kubernetes.client.models.V1beta2Deployment;
import io.kubernetes.client.models.V1beta2ReplicaSet;

public interface V1beta2ResourceUpdateMonitor {


    V1beta2ResourceUpdateMonitor NOOP = new Adapter();

    void onDeploymentUpdate(V1beta2Deployment original, V1beta2Deployment current);

    void onDaemonSetUpdate(V1beta2DaemonSet original, V1beta2DaemonSet current);

    void onReplicaSetUpdate(V1beta2ReplicaSet original, V1beta2ReplicaSet current);

    class Adapter implements V1beta2ResourceUpdateMonitor {

        @Override
        public void onDeploymentUpdate(V1beta2Deployment original, V1beta2Deployment current) {
        }

        @Override
        public void onDaemonSetUpdate(V1beta2DaemonSet original, V1beta2DaemonSet current) {
        }

        @Override
        public void onReplicaSetUpdate(V1beta2ReplicaSet original, V1beta2ReplicaSet current) {
        }
    }
}



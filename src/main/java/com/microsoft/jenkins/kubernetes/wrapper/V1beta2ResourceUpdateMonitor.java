package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.models.V1beta2DaemonSet;
import io.kubernetes.client.models.V1beta2Deployment;

public interface V1beta2ResourceUpdateMonitor {


    void onDeploymentUpdate(V1beta2Deployment original, V1beta2Deployment current);

    void onDaemonSetUpdate(V1beta2DaemonSet original, V1beta2DaemonSet current);

    V1beta2ResourceUpdateMonitor NOOP = new Adapter();

    class Adapter implements V1beta2ResourceUpdateMonitor {

        @Override
        public void onDeploymentUpdate(V1beta2Deployment original, V1beta2Deployment current) {
        }
        @Override
        public void onDaemonSetUpdate(V1beta2DaemonSet original, V1beta2DaemonSet current) {
        }
    }
}


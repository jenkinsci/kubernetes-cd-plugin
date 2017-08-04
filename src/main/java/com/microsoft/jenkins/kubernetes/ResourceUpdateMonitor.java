/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;

public interface ResourceUpdateMonitor {
    boolean isInterestedInSecret();

    void onSecretUpdate(Secret original, Secret current);

    boolean isInterestedInDeployment();

    void onDeploymentUpdate(Deployment original, Deployment current);

    boolean isInterestedInService();

    void onServiceUpdate(Service original, Service current);

    boolean isInterestedInIngress();

    void onIngressUpdate(Ingress original, Ingress current);

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
    }
}

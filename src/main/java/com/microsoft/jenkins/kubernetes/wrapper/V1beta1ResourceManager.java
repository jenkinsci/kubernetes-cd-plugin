/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1beta1Api;
import io.kubernetes.client.apis.BatchV1beta1Api;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.AppsV1beta1Deployment;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.V1Status;
import io.kubernetes.client.models.V1beta1CronJob;
import io.kubernetes.client.models.V1beta1DaemonSet;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta1ReplicaSet;
import io.kubernetes.client.models.V1beta1StatefulSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class V1beta1ResourceManager extends ResourceManager {
    private final ExtensionsV1beta1Api extensionsV1beta1Api;
    private final AppsV1beta1Api appsV1beta1Api;
    private final BatchV1beta1Api batchV1beta1Api;
    private V1beta1ResourceUpdateMonitor resourceUpdateMonitor = V1beta1ResourceUpdateMonitor.NOOP;

    public V1beta1ResourceManager(ApiClient client) {
        super(true);
        checkNotNull(client);
        extensionsV1beta1Api = new ExtensionsV1beta1Api(client);
        appsV1beta1Api = new AppsV1beta1Api(client);
        batchV1beta1Api = new BatchV1beta1Api(client);
    }

    public V1beta1ResourceManager(ApiClient client, boolean pretty) {
        super(pretty);
        checkNotNull(client);
        extensionsV1beta1Api = new ExtensionsV1beta1Api(client);
        appsV1beta1Api = new AppsV1beta1Api(client);
        batchV1beta1Api = new BatchV1beta1Api(client);
    }

    public V1beta1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V1beta1ResourceManager withResourceUpdateMonitor(V1beta1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    class ReplicaSetUpdater extends ResourceUpdater<V1beta1ReplicaSet> {
        ReplicaSetUpdater(V1beta1ReplicaSet replicaSet) {
            super(replicaSet);
        }

        @Override
        V1beta1ReplicaSet getCurrentResource() {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1Api.readNamespacedReplicaSet(getName(), getNamespace(),
                        getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return replicaSet;
        }

        @Override
        V1beta1ReplicaSet applyResource(V1beta1ReplicaSet original, V1beta1ReplicaSet current) {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1Api.replaceNamespacedReplicaSet(getName(), getNamespace(),
                        current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1beta1ReplicaSet createResource(V1beta1ReplicaSet current) {
            V1beta1ReplicaSet replicaSet = null;
            try {
                replicaSet = extensionsV1beta1Api.createNamespacedReplicaSet(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return replicaSet;
        }

        @Override
        V1Status deleteResource(V1beta1ReplicaSet current) {
            V1Status result = null;
            try {
                result = extensionsV1beta1Api.deleteNamespacedReplicaSet(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1beta1ReplicaSet original, V1beta1ReplicaSet current) {
            resourceUpdateMonitor.onReplicaSetUpdate(original, current);
        }
    }

    class DaemonSetUpdater extends ResourceUpdater<V1beta1DaemonSet> {
        DaemonSetUpdater(V1beta1DaemonSet daemonSet) {
            super(daemonSet);
        }

        @Override
        V1beta1DaemonSet getCurrentResource() {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = extensionsV1beta1Api.readNamespacedDaemonSet(getName(), getNamespace(),
                        getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return daemonSet;
        }

        @Override
        V1beta1DaemonSet applyResource(V1beta1DaemonSet original, V1beta1DaemonSet current) {
            V1beta1DaemonSet daemonSet = null;
            try {

                daemonSet = extensionsV1beta1Api.replaceNamespacedDaemonSet(getName(), getNamespace(),
                        current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1beta1DaemonSet createResource(V1beta1DaemonSet current) {
            V1beta1DaemonSet daemonSet = null;
            try {
                daemonSet = extensionsV1beta1Api.createNamespacedDaemonSet(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return daemonSet;
        }

        @Override
        V1Status deleteResource(V1beta1DaemonSet current) {
            V1Status result = null;
            try {
                result = extensionsV1beta1Api.deleteNamespacedDaemonSet(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1beta1DaemonSet original, V1beta1DaemonSet current) {
            resourceUpdateMonitor.onDaemonSetUpdate(original, current);
        }
    }


    class IngressUpdater extends ResourceUpdater<V1beta1Ingress> {
        IngressUpdater(V1beta1Ingress ingress) {
            super(ingress);
        }

        @Override
        V1beta1Ingress getCurrentResource() {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1Api.readNamespacedIngress(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return ingress;
        }

        @Override
        V1beta1Ingress applyResource(V1beta1Ingress original, V1beta1Ingress current) {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1Api.replaceNamespacedIngress(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return ingress;
        }

        @Override
        V1beta1Ingress createResource(V1beta1Ingress current) {
            V1beta1Ingress ingress = null;
            try {
                ingress = extensionsV1beta1Api.createNamespacedIngress(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return ingress;
        }

        @Override
        V1Status deleteResource(V1beta1Ingress current) {
            V1Status result = null;
            try {
                result = extensionsV1beta1Api.deleteNamespacedIngress(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }


        @Override
        void notifyUpdate(V1beta1Ingress original, V1beta1Ingress current) {
            resourceUpdateMonitor.onIngressUpdate(original, current);
        }
    }

    class ExtensionsDeploymentUpdater extends ResourceUpdater<ExtensionsV1beta1Deployment> {
        ExtensionsDeploymentUpdater(ExtensionsV1beta1Deployment deployment) {
            super(deployment);
        }

        @Override
        ExtensionsV1beta1Deployment getCurrentResource() {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1Api.readNamespacedDeployment(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return deployment;
        }

        @Override
        ExtensionsV1beta1Deployment applyResource(ExtensionsV1beta1Deployment original,
                                                  ExtensionsV1beta1Deployment current) {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1Api.replaceNamespacedDeployment(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        ExtensionsV1beta1Deployment createResource(ExtensionsV1beta1Deployment current) {
            ExtensionsV1beta1Deployment deployment = null;
            try {
                deployment = extensionsV1beta1Api.createNamespacedDeployment(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        V1Status deleteResource(ExtensionsV1beta1Deployment current) {
            V1Status result = null;
            try {
                result = extensionsV1beta1Api.deleteNamespacedDeployment(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }


        @Override
        void notifyUpdate(ExtensionsV1beta1Deployment original, ExtensionsV1beta1Deployment current) {
            resourceUpdateMonitor.onDeploymentUpdate(original, current);
        }
    }

    class AppsDeploymentUpdater extends ResourceUpdater<AppsV1beta1Deployment> {
        AppsDeploymentUpdater(AppsV1beta1Deployment deployment) {
            super(deployment);
        }

        @Override
        AppsV1beta1Deployment getCurrentResource() {
            AppsV1beta1Deployment deployment = null;
            try {
                deployment = appsV1beta1Api.readNamespacedDeployment(getName(), getNamespace(), getPretty(),
                        true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return deployment;
        }

        @Override
        AppsV1beta1Deployment applyResource(AppsV1beta1Deployment original,
                                            AppsV1beta1Deployment current) {
            AppsV1beta1Deployment deployment = null;
            try {
                deployment = appsV1beta1Api.replaceNamespacedDeployment(getName(), getNamespace(), current,
                        getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        AppsV1beta1Deployment createResource(AppsV1beta1Deployment current) {
            AppsV1beta1Deployment deployment = null;
            try {
                deployment = appsV1beta1Api.createNamespacedDeployment(getNamespace(),
                        current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return deployment;
        }

        @Override
        V1Status deleteResource(AppsV1beta1Deployment current) {
            V1Status result = null;
            try {
                result = appsV1beta1Api.deleteNamespacedDeployment(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(AppsV1beta1Deployment original, AppsV1beta1Deployment current) {
            resourceUpdateMonitor.onDeploymentUpdate(original, current);
        }
    }

    class StatefulSetUpdater extends ResourceUpdater<V1beta1StatefulSet> {
        StatefulSetUpdater(V1beta1StatefulSet namespace) {
            super(namespace);
        }

        @Override
        V1beta1StatefulSet getCurrentResource() {
            V1beta1StatefulSet result = null;
            try {
                result = appsV1beta1Api.readNamespacedStatefulSet(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V1beta1StatefulSet applyResource(V1beta1StatefulSet original, V1beta1StatefulSet current) {
            V1beta1StatefulSet result = null;
            try {

                result = appsV1beta1Api.replaceNamespacedStatefulSet(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1beta1StatefulSet createResource(V1beta1StatefulSet current) {
            V1beta1StatefulSet result = null;
            try {
                result = appsV1beta1Api.createNamespacedStatefulSet(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V1beta1StatefulSet current) {
            V1Status result = null;
            try {
                result = appsV1beta1Api.deleteNamespacedStatefulSet(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1beta1StatefulSet original, V1beta1StatefulSet current) {
            resourceUpdateMonitor.onStatefulSetUpdate(original, current);
        }
    }

    class CronJobUpdater extends ResourceUpdater<V1beta1CronJob> {
        CronJobUpdater(V1beta1CronJob namespace) {
            super(namespace);
        }

        @Override
        V1beta1CronJob getCurrentResource() {
            V1beta1CronJob result = null;
            try {
                result = batchV1beta1Api.readNamespacedCronJob(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V1beta1CronJob applyResource(V1beta1CronJob original, V1beta1CronJob current) {
            V1beta1CronJob result = null;
            try {
                result = batchV1beta1Api.replaceNamespacedCronJob(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1beta1CronJob createResource(V1beta1CronJob current) {
            V1beta1CronJob result = null;
            try {
                result = batchV1beta1Api.createNamespacedCronJob(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V1beta1CronJob current) {
            V1Status result = null;
            try {
                result = batchV1beta1Api.deleteNamespacedCronJob(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V1beta1CronJob original, V1beta1CronJob current) {
            resourceUpdateMonitor.onCronJobUpdate(original, current);
        }
    }

}

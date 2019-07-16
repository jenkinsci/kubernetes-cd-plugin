package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.BatchV2alpha1Api;
import io.kubernetes.client.models.V1Status;
import io.kubernetes.client.models.V2alpha1CronJob;

import static com.google.common.base.Preconditions.checkNotNull;


public class V2alpha1ResourceManager extends ResourceManager {
    private final BatchV2alpha1Api batchV2alpha1Api;

    private V2alpha1ResourceUpdateMonitor resourceUpdateMonitor = V2alpha1ResourceUpdateMonitor.NOOP;

    public V2alpha1ResourceManager(ApiClient client) {
        super(true);
        checkNotNull(client);

        batchV2alpha1Api = new BatchV2alpha1Api(client);

    }

    public V2alpha1ResourceManager(ApiClient client, boolean pretty) {
        super(pretty);
        checkNotNull(client);

        batchV2alpha1Api = new BatchV2alpha1Api(client);
    }

    public V2alpha1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V2alpha1ResourceManager withResourceUpdateMonitor(V2alpha1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    class CronJobUpdater extends ResourceUpdater<V2alpha1CronJob> {
        CronJobUpdater(V2alpha1CronJob namespace) {
            super(namespace);
        }

        @Override
        V2alpha1CronJob getCurrentResource() {
            V2alpha1CronJob result = null;
            try {
                result = batchV2alpha1Api.readNamespacedCronJob(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V2alpha1CronJob applyResource(V2alpha1CronJob original, V2alpha1CronJob current) {
            V2alpha1CronJob result = null;
            try {
                result = batchV2alpha1Api.replaceNamespacedCronJob(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V2alpha1CronJob createResource(V2alpha1CronJob current) {
            V2alpha1CronJob result = null;
            try {
                result = batchV2alpha1Api.createNamespacedCronJob(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V1Status deleteResource(V2alpha1CronJob current) {
            V1Status result = null;
            try {
                result = batchV2alpha1Api.deleteNamespacedCronJob(
                        getName(), getNamespace(), getPretty(), null, null, null, null, null);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V2alpha1CronJob original, V2alpha1CronJob current) {
            resourceUpdateMonitor.onCronJobUpdate(original, current);
        }
    }
}

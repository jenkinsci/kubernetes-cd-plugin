package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AutoscalingV2beta1Api;
import io.kubernetes.client.models.V2beta1HorizontalPodAutoscaler;

import static com.google.common.base.Preconditions.checkNotNull;

public class V2beta1ResourceManager extends ResourceManager {
    private final AutoscalingV2beta1Api autoscalingV2beta1Api;
    private final AutoscalingV2beta1Api autoscalingV2beta1ApiPatch;

    private V2beta1ResourceUpdateMonitor resourceUpdateMonitor = V2beta1ResourceUpdateMonitor.NOOP;

    public V2beta1ResourceManager(ApiClient client, ApiClient strategicPatchClient) {
        super(true);
        checkNotNull(client);
        checkNotNull(strategicPatchClient);

        autoscalingV2beta1Api = new AutoscalingV2beta1Api(client);
        autoscalingV2beta1ApiPatch = new AutoscalingV2beta1Api(strategicPatchClient);

    }

    public V2beta1ResourceManager(ApiClient client, ApiClient strategicPatchClient, boolean pretty) {
        super(pretty);
        checkNotNull(client);
        checkNotNull(strategicPatchClient);

        autoscalingV2beta1Api = new AutoscalingV2beta1Api(client);
        autoscalingV2beta1ApiPatch = new AutoscalingV2beta1Api(strategicPatchClient);
    }

    public V2beta1ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V2beta1ResourceManager withResourceUpdateMonitor(V2beta1ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    class HorizontalPodAutoscalerUpdater extends ResourceUpdater<V2beta1HorizontalPodAutoscaler> {
        HorizontalPodAutoscalerUpdater(V2beta1HorizontalPodAutoscaler namespace) {
            super(namespace);
        }

        @Override
        V2beta1HorizontalPodAutoscaler getCurrentResource() {
            V2beta1HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV2beta1Api.readNamespacedHorizontalPodAutoscaler(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V2beta1HorizontalPodAutoscaler applyResource(
                V2beta1HorizontalPodAutoscaler original, V2beta1HorizontalPodAutoscaler current) {
            V2beta1HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV2beta1ApiPatch.patchNamespacedHorizontalPodAutoscaler(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V2beta1HorizontalPodAutoscaler createResource(V2beta1HorizontalPodAutoscaler current) {
            V2beta1HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV2beta1Api.createNamespacedHorizontalPodAutoscaler(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V2beta1HorizontalPodAutoscaler original, V2beta1HorizontalPodAutoscaler current) {
            resourceUpdateMonitor.onHorizontalPodAutoscalerUpdate(original, current);
        }
    }
}

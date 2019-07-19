package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.models.V2beta2HorizontalPodAutoscaler;

public interface V2beta2ResourceUpdateMonitor {


    V2beta2ResourceUpdateMonitor NOOP = new Adapter();

    void onHorizontalPodAutoscalerUpdate(
            V2beta2HorizontalPodAutoscaler original, V2beta2HorizontalPodAutoscaler current);

    class Adapter implements V2beta2ResourceUpdateMonitor {

        @Override
        public void onHorizontalPodAutoscalerUpdate(
                V2beta2HorizontalPodAutoscaler original, V2beta2HorizontalPodAutoscaler current) {
        }

    }
}

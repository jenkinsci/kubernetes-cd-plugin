package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.models.V2beta1HorizontalPodAutoscaler;

public interface V2beta1ResourceUpdateMonitor {


    V2beta1ResourceUpdateMonitor NOOP = new Adapter();

    void onHorizontalPodAutoscalerUpdate(
            V2beta1HorizontalPodAutoscaler original, V2beta1HorizontalPodAutoscaler current);

    class Adapter implements V2beta1ResourceUpdateMonitor {

        @Override
        public void onHorizontalPodAutoscalerUpdate(
                V2beta1HorizontalPodAutoscaler original, V2beta1HorizontalPodAutoscaler current) {
        }

    }
}

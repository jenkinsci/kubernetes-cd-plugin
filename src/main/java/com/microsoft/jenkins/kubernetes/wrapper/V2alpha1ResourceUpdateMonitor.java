package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.models.V2alpha1CronJob;

public interface V2alpha1ResourceUpdateMonitor {


    V2alpha1ResourceUpdateMonitor NOOP = new Adapter();

    void onCronJobUpdate(
            V2alpha1CronJob original, V2alpha1CronJob current);

    class Adapter implements V2alpha1ResourceUpdateMonitor {

        @Override
        public void onCronJobUpdate(
                V2alpha1CronJob original, V2alpha1CronJob current) {
        }

    }
}

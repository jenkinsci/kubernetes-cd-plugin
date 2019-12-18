package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.openapi.models.AppsV1beta1Deployment;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.openapi.models.NetworkingV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicationController;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import io.kubernetes.client.openapi.models.V1beta1DaemonSet;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1beta1ReplicaSet;
import io.kubernetes.client.openapi.models.V1beta1StatefulSet;
import io.kubernetes.client.openapi.models.V1beta2DaemonSet;
import io.kubernetes.client.openapi.models.V1beta2Deployment;
import io.kubernetes.client.openapi.models.V1beta2ReplicaSet;
import io.kubernetes.client.openapi.models.V1beta2StatefulSet;
import io.kubernetes.client.openapi.models.V2alpha1CronJob;
import io.kubernetes.client.openapi.models.V2beta1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V2beta2HorizontalPodAutoscaler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public final class ResourceUpdaterMap extends HashMap<Class<?>,
        Pair<Class<? extends ResourceManager>, Class<? extends ResourceManager.ResourceUpdater>>> {
    private static final Map<Class<?>,
            Pair<Class<? extends ResourceManager>, Class<? extends ResourceManager.ResourceUpdater>>> INSTANCE =
            Collections.unmodifiableMap(new ResourceUpdaterMap());

    private ResourceUpdaterMap() {
        put(V1Namespace.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.NamespaceUpdater.class));
        put(V1Deployment.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.DeploymentUpdater.class));
        put(V1Service.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.ServiceUpdater.class));
        put(V1ReplicationController.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.ReplicationControllerUpdater.class));
        put(V1DaemonSet.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.DaemonSetUpdater.class));
        put(V1Job.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.JobUpdater.class));
        put(V1Pod.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.PodUpdater.class));
        put(V1HorizontalPodAutoscaler.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.HorizontalPodAutoscalerUpdater.class));
        put(V1Secret.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.SecretUpdater.class));
        put(V1ConfigMap.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.ConfigMapUpdater.class));
        put(V1ReplicaSet.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.ReplicaSetUpdater.class));
        put(V1StatefulSet.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.StatefulSetUpdater.class));
        put(V1PersistentVolumeClaim.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.PersistentVolumeClaimUpdater.class));
        put(V1PersistentVolume.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.PersistentVolumeUpdater.class));
        put(V1NetworkPolicy.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.NetworkPolicyUpdater.class));
        put(V1Role.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.RoleUpdater.class));
        put(V1RoleBinding.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.RoleBindingUpdater.class));
        put(V1ServiceAccount.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.ServiceAccountUpdater.class));
        put(V1ClusterRole.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.ClusterRoleUpdater.class));
        put(V1ClusterRoleBinding.class,
                Pair.of(V1ResourceManager.class, V1ResourceManager.ClusterRoleBindingUpdater.class));



        put(ExtensionsV1beta1Ingress.class,
                Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.ExtensionsIngressUpdater.class));
        put(NetworkingV1beta1Ingress.class,
                Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.NetworkingIngressUpdater.class));
        put(V1beta1DaemonSet.class,
                Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.DaemonSetUpdater.class));
        put(V1beta1ReplicaSet.class,
                Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.ReplicaSetUpdater.class));
        put(V1beta1StatefulSet.class,
                Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.StatefulSetUpdater.class));
        put(V1beta1CronJob.class,
                Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.CronJobUpdater.class));
        put(ExtensionsV1beta1Deployment.class,
                Pair.of(V1beta1ResourceManager.class,
                        V1beta1ResourceManager.ExtensionsDeploymentUpdater.class));
        put(AppsV1beta1Deployment.class,
                Pair.of(V1beta1ResourceManager.class,
                        V1beta1ResourceManager.AppsDeploymentUpdater.class));

        put(V1beta2Deployment.class,
                Pair.of(V1beta2ResourceManager.class,
                        V1beta2ResourceManager.DeploymentUpdater.class));
        put(V1beta2DaemonSet.class,
                Pair.of(V1beta2ResourceManager.class,
                        V1beta2ResourceManager.DaemonSetUpdater.class));
        put(V1beta2ReplicaSet.class,
                Pair.of(V1beta2ResourceManager.class,
                        V1beta2ResourceManager.ReplicaSetUpdater.class));
        put(V1beta2StatefulSet.class,
                Pair.of(V1beta2ResourceManager.class,
                        V1beta2ResourceManager.StatefulSetUpdater.class));

        put(V2beta1HorizontalPodAutoscaler.class,
                Pair.of(V2beta1ResourceManager.class, V2beta1ResourceManager.HorizontalPodAutoscalerUpdater.class));

        put(V2beta2HorizontalPodAutoscaler.class,
                Pair.of(V2beta2ResourceManager.class, V2beta2ResourceManager.HorizontalPodAutoscalerUpdater.class));

        put(V2alpha1CronJob.class,
                Pair.of(V2alpha1ResourceManager.class, V2alpha1ResourceManager.CronJobUpdater.class));
    }

    public static Map<Class<?>, Pair<Class<? extends ResourceManager>,
            Class<? extends ResourceManager.ResourceUpdater>>> getUnmodifiableInstance() {
        return INSTANCE;
    }
}

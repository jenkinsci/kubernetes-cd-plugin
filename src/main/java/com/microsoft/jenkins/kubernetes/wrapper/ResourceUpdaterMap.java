package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1DaemonSet;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1ReplicaSet;
import io.kubernetes.client.models.V1ReplicationController;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1beta1DaemonSet;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta1ReplicaSet;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ResourceUpdaterMap extends HashMap<Class<?>, Pair<Class<? extends ResourceManager>, Class<? extends ResourceManager.ResourceUpdater>>> {
    private static final Map<Class<?>, Pair<Class<? extends ResourceManager>, Class<? extends ResourceManager.ResourceUpdater>>> INSTANCE =
            Collections.unmodifiableMap(new ResourceUpdaterMap());

    private ResourceUpdaterMap() {
        put(V1Namespace.class, Pair.of(V1ResourceManager.class, V1ResourceManager.NamespaceUpdater.class));
        put(V1Deployment.class, Pair.of(V1ResourceManager.class, V1ResourceManager.DeploymentUpdater.class));
        put(V1Service.class, Pair.of(V1ResourceManager.class, V1ResourceManager.ServiceUpdater.class));
        put(V1ReplicationController.class, Pair.of(V1ResourceManager.class, V1ResourceManager.ReplicationControllerUpdater.class));
        put(V1DaemonSet.class, Pair.of(V1ResourceManager.class, V1ResourceManager.DaemonSetUpdater.class));
        put(V1Job.class, Pair.of(V1ResourceManager.class, V1ResourceManager.JobUpdater.class));
        put(V1Pod.class, Pair.of(V1ResourceManager.class, V1ResourceManager.PodUpdater.class));
        put(V1Secret.class, Pair.of(V1ResourceManager.class, V1ResourceManager.SecretUpdater.class));
        put(V1ConfigMap.class, Pair.of(V1ResourceManager.class, V1ResourceManager.ConfigMapUpdater.class));


        put(V1ConfigMap.class, Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.IngressUpdater.class));
        put(V1ConfigMap.class, Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.DaemonSetUpdater.class));
        put(V1ConfigMap.class, Pair.of(V1beta1ResourceManager.class, V1beta1ResourceManager.ReplicaSetUpdater.class));

        put(V1ConfigMap.class, Pair.of(ExtensionV1beta1ResourceManager.class, ExtensionV1beta1ResourceManager.DeploymentUpdater.class));

    }

    public static Map<Class<?>, Pair<Class<? extends ResourceManager>, Class<? extends ResourceManager.ResourceUpdater>>> getUnmodifiableInstance() {
        return INSTANCE;
    }
}

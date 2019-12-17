package com.microsoft.jenkins.kubernetes.util;

import io.kubernetes.client.openapi.JSON;

public final class KubernetesJsonUtils {
    private static final JSON KUBERNETES_JSON = new JSON();

    private KubernetesJsonUtils() {

    }

    public static JSON getKubernetesJson() {
        return KUBERNETES_JSON;
    }
}

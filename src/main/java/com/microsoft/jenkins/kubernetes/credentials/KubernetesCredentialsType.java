/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

public enum KubernetesCredentialsType {
    KubeConfig("Authenticate with kubeconfig file in workspace"),
    SSH("Fetch cluster details through SSH connection to the master node"),
    Text("Fill credentials details directly");

    public static final KubernetesCredentialsType DEFAULT = KubeConfig;

    private final String title;

    KubernetesCredentialsType(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public static KubernetesCredentialsType fromString(String value) {
        if ("text".equalsIgnoreCase(value)) {
            return Text;
        }
        if ("ssh".equalsIgnoreCase(value)) {
            return SSH;
        }
        return KubeConfig;
    }
}

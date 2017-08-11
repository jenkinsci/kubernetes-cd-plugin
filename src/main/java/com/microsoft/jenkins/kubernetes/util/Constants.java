/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.util;

import java.util.regex.Pattern;

public final class Constants {
    public static final String INVALID_OPTION = "*";

    public static final String DEFAULT_KUBERNETES_NAMESPACE = "default";

    public static final String KUBECONFIG_FILE = ".kube/config";

    public static final String KUBECONFIG_PREFIX = "kubeconfig-";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String HTTPS_PREFIX = "https://";

    /**
     * Length limit for the Kubernetes names.
     *
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/names/">
     * Kubernetes Names
     * </a>
     */
    public static final int KUBERNETES_NAME_LENGTH_LIMIT = 253;

    /**
     * Pattern for the Kubernetes names.
     *
     * @see <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/names/">
     * Kubernetes Names
     * </a>
     */
    public static final Pattern KUBERNETES_NAME_PATTERN =
            Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$");

    public static final String KUBERNETES_SECRET_NAME_PREFIX = "acs-plugin-";
    public static final String KUBERNETES_SECRET_NAME_PROP = "KUBERNETES_SECRET_NAME";

    public static final int DEFAULT_SSH_PORT = 22;

    // AI constants
    public static final String AI_KUBERNETES = "Kubernetes";
    public static final String AI_K8S_MASTER = "Master";
    public static final String AI_MESSAGE = "ErrorMessage";

    private Constants() {
        // hide constructor
    }
}

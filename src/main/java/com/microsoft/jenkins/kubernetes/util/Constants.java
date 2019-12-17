/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.util;

import io.kubernetes.client.openapi.models.V1DeleteOptions;

import java.util.regex.Pattern;

public final class Constants {
    public static final String INVALID_OPTION = "*";

    public static final String DEFAULT_KUBERNETES_NAMESPACE = "default";

    public static final String KUBECONFIG_FILE = ".kube/config";

    public static final String KUBECONFIG_PREFIX = "kubeconfig-";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String HTTPS_PREFIX = "https://";

    public static final String KUBERNETES_CONTROLLER_UID_FIELD = "controller-uid";

    public static final String KUBERNETES_JOB_NAME_FIELD = "job-name";

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

    public static final String DRY_RUN_ALL = "All";

    public static final int DEFAULT_SSH_PORT = 22;

    // AI constants
    public static final String AI_KUBERNETES = "Kubernetes";
    public static final String AI_K8S_MASTER = "K8sMaster";

    // https://kubernetes.io/docs/concepts/workloads/controllers/garbage-collection/#background-cascading-deletion

    public static final V1DeleteOptions BACKGROUND_DELETEION = new V1DeleteOptions().propagationPolicy("Background");


    /**
     * URI scheme prefix (scheme://) pattern.
     * <p>
     * The scheme consists of a sequence of characters beginning with a letter and followed by any combination of
     * letters, digits, plus (+), period (.), or hyphen (-).
     */
    public static final Pattern URI_SCHEME_PREFIX =
            Pattern.compile("^[a-z][a-z0-9+.\\-]*://", Pattern.CASE_INSENSITIVE);

    private Constants() {
        // hide constructor
    }
}

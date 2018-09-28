/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import com.microsoft.jenkins.kubernetes.util.CommonUtils;
import com.microsoft.jenkins.kubernetes.util.Constants;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1ObjectMeta;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class ResourceManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * If true, then the output of api call is pretty printed.
     */
    protected final String pretty;

    ResourceManager(boolean pretty) {
        this.pretty = String.valueOf(pretty);
    }

    protected abstract class ResourceUpdater<T> {
        private final T resource;
        private final V1ObjectMeta metadata;

        ResourceUpdater(T resource) {
            checkNotNull(resource);
            this.resource = resource;
            V1ObjectMeta meta = null;
            try {
                Method method = resource.getClass().getMethod("getMetadata");
                meta = (V1ObjectMeta) method.invoke(resource);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                logger.error(String.format("Fail to fetch meta data for %s", resource));
            }
            metadata = meta;
            checkState(StringUtils.isNotBlank(getName()),
                    Messages.KubernetesClientWrapper_noName(), getKind(), resource);
        }

        final String getNamespace() {
            if (metadata != null) {
                return metadata.getNamespace();
            }
            return Constants.DEFAULT_KUBERNETES_NAMESPACE;
        }

        final T get() {
            return resource;
        }

        final String getName() {
            String name = null;
            if (metadata != null) {
                name = metadata.getName();
            }
            return name;
        }

        @Deprecated
        final String getKind() {
            return "GetKind not support in this version";
        }

        /**
         * Explicitly apply the configuration if a resource with the same name exists in the namespace in the cluster,
         * or create one if not.
         * <p>
         * If we cannot load resource during application (possibly because the resource gets deleted after we first
         * checked), or some one created the resource after we checked and before we created, the method fails with
         * exception.
         *
         * @throws IOException if we cannot find the resource in the cluster when we apply the configuration
         */
        final void createOrApply() throws IOException {
            T original = getCurrentResource();
            T current = get();
            T updated;
            if (original != null) {
                updated = applyResource(original, current);
                if (updated == null) {
                    throw new IOException(Messages.KubernetesClientWrapper_resourceNotFound(
                            getKind(), CommonUtils.getResourceName(current)));
                }
                logApplied(updated);
            } else {
                updated = createResource(get());
                logCreated(updated);
            }
            notifyUpdate(original, updated);
        }

        abstract T getCurrentResource();

        abstract T applyResource(T original, T current);

        abstract T createResource(T current);

        abstract void notifyUpdate(T original, T current);

        void logApplied(T res) {
            getLogger().info(Messages.KubernetesClientWrapper_applied(res.getClass().getSimpleName(), res));
        }

        void logCreated(T res) {
            getLogger().info(Messages.KubernetesClientWrapper_created(res.getClass().getSimpleName(), res));
        }
    }

    public Logger getLogger() {
        return logger;
    }

    protected void handleApiException(ApiException e) {
        int code = e.getCode();
        String responseBody = e.getResponseBody();
        getLogger().error(Messages.KubernetesClientWrapper_apiException(code, responseBody));
    }

    /**
     * Check the resource object is matched in the API version, if matched, apply the action defined in the resource.
     *
     * @param resource
     * @return true if resource matched
     * @throws IOException
     */
    public abstract boolean apply(Object resource) throws IOException;
}

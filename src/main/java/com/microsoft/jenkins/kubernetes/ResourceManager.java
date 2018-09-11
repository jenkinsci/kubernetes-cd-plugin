/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.microsoft.jenkins.kubernetes.util.CommonUtils;
import com.microsoft.jenkins.kubernetes.util.Constants;
import io.kubernetes.client.models.V1ObjectMeta;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class ResourceManager {
    protected static final String DEFAULT_PRETTY = "true";

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
                // log
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

        final String getKind() {
//            return resource.getKind();
            return null;
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
            log(Messages.KubernetesClientWrapper_applied(res.getClass().getSimpleName(), res));
        }

        void logCreated(T res) {
            log(Messages.KubernetesClientWrapper_created(res.getClass().getSimpleName(), res));
        }
    }

    protected void log(String s) {
    }

    /**
     * Check the resource object is matched in the API version, if matched, apply the action defined in the resource.
     * @param resource
     * @return true if resource matched
     * @throws IOException
     */
    public abstract boolean apply(Object resource) throws IOException;
}

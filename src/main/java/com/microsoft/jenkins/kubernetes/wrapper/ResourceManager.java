/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import com.google.gson.JsonSyntaxException;
import com.microsoft.jenkins.kubernetes.util.Constants;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1Status;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class ResourceManager {
    /**
     * If true, then the output of api call is pretty printed.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);
    private final String pretty;
    private PrintStream consoleLogger = System.out;

    ResourceManager(boolean pretty) {
        this.pretty = String.valueOf(pretty);
    }

    public String getPretty() {
        return pretty;
    }

    public PrintStream getConsoleLogger() {
        return consoleLogger;
    }

    public ResourceManager setConsoleLogger(PrintStream log) {
        this.consoleLogger = log;
        return this;
    }

    /**
     * Func to handle ApiException, print out the contents of the exception
     * and throw a RuntimeException to abort the pipeline .
     *
     * @param e kubernetes ApiException
     * @throws RuntimeException
     */
    protected void handleApiException(ApiException e) throws RuntimeException {
        int code = e.getCode();
        String responseBody = e.getResponseBody();
        getConsoleLogger().println(Messages.KubernetesClientWrapper_apiException(code, responseBody));
        throw new RuntimeException(e);
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
                consoleLogger.println(String.format("Fail to fetch meta data for %s", resource));
            }
            metadata = meta;
            checkState(StringUtils.isNotBlank(getName()),
                    Messages.KubernetesClientWrapper_noName(), getKind(), resource);
        }

        final String getNamespace() {
            if (metadata != null) {
                if (metadata.getNamespace() != null) {
                    return metadata.getNamespace();
                }
                return Constants.DEFAULT_KUBERNETES_NAMESPACE;
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
            return resource.getClass().getSimpleName();
        }

        /**
         * Explicitly apply the configuration if a resource with the same name exists in the namespace in the cluster,
         * or create one if not.
         */
        final void createOrApply() {
            T original = getCurrentResource();
            T current = get();
            T updated;
            if (original != null) {
                updated = applyResource(original, current);
                logApplied(updated);
            } else {
                updated = createResource(get());
                logCreated(updated);
            }
            notifyUpdate(original, updated);
        }

        final void delete() {
            try {
                V1Status status = deleteResource(get());
                logDeleted(status);
            } catch (JsonSyntaxException e) {
                if (e.getCause() instanceof IllegalStateException) {
                    IllegalStateException ise = (IllegalStateException) e.getCause();
                    if (ise.getMessage() != null && ise.getMessage().contains(
                            "Expected a string but was BEGIN_OBJECT")) {
                        LOGGER.debug("Catching exception because of issue "
                                + "https://github.com/kubernetes-client/java/issues/86", e);
                        consoleLogger.println(Messages.KubernetesClientWrapper_deleted(get(), null));
                    } else {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }

        }

        abstract T getCurrentResource();

        abstract T applyResource(T original, T current);

        abstract T createResource(T current);

        abstract V1Status deleteResource(T current);

        abstract void notifyUpdate(T original, T current);

        void logApplied(T res) {
            getConsoleLogger().println(Messages.KubernetesClientWrapper_applied(res.getClass().getSimpleName(), res));
        }

        void logCreated(T res) {
            getConsoleLogger().println(Messages.KubernetesClientWrapper_created(res.getClass().getSimpleName(), res));
        }


        void logDeleted(V1Status status) {
            if (status != null) {
                getConsoleLogger().println(
                        Messages.KubernetesClientWrapper_deleted(get().getClass().getSimpleName(), status));
            } else {
                getConsoleLogger().println(
                        Messages.KubernetesClientWrapper_resourceNotFound(get().getClass().getSimpleName(), getName()));
            }
        }


        /**
         * Func to handle ApiException , print out the contents of the exception
         * and throw a RuntimeException to abort the pipeline except NotFound Condition.
         *
         * @param e kubernetes ApiException
         * @throws RuntimeException
         */
        protected void handleApiExceptionExceptNotFound(ApiException e) throws RuntimeException {
            int code = e.getCode();
            if (code == HttpStatus.SC_NOT_FOUND) {
                return;
            }
            String responseBody = e.getResponseBody();
            getConsoleLogger().println(Messages.KubernetesClientWrapper_apiException(code, responseBody));
            throw new RuntimeException(e);
        }

        /**
         * Func to handle ApiException, print out the contents of the exception
         * and throw a RuntimeException to abort the pipeline .
         *
         * @param e kubernetes ApiException
         * @throws RuntimeException
         */
        protected void handleApiException(ApiException e) throws RuntimeException {
            int code = e.getCode();
            String responseBody = e.getResponseBody();
            getConsoleLogger().println(Messages.KubernetesClientWrapper_apiException(code, responseBody));
            throw new RuntimeException(e);
        }
    }
}

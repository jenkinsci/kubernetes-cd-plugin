/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.CommonUtils;
import com.microsoft.jenkins.kubernetes.util.Constants;
import com.microsoft.jenkins.kubernetes.util.DockerConfigBuilder;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.util.VariableResolver;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1SecretBuilder;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KubernetesClientWrapper {
    private final ApiClient client;
    private PrintStream logger = System.out;
    private VariableResolver<String> variableResolver;

    @VisibleForTesting
    KubernetesClientWrapper(ApiClient client) {
        this.client = client;
        Configuration.setDefaultApiClient(client);
    }

    public KubernetesClientWrapper(String kubeConfig) {
        File file = new File(kubeConfig);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                kubeConfig = IOUtils.toString(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        StringReader reader = new StringReader(kubeConfig);
        try {
            client = Config.fromConfig(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Configuration.setDefaultApiClient(client);
    }

    public KubernetesClientWrapper(String server,
                                   String certificateAuthorityData,
                                   String clientCertificateData,
                                   String clientKeyData) {
        ClientCertificateAuthentication authentication = new ClientCertificateAuthentication(
                clientCertificateData.getBytes(StandardCharsets.UTF_8), clientKeyData.getBytes(StandardCharsets.UTF_8));
        client = new ClientBuilder()
                .setBasePath(server)
                .setAuthentication(authentication)
                .setCertificateAuthority(certificateAuthorityData.getBytes(StandardCharsets.UTF_8))
                .build();
        Configuration.setDefaultApiClient(client);
    }

    public ApiClient getClient() {
        return client;
    }

    public PrintStream getLogger() {
        return logger;
    }

    public KubernetesClientWrapper withLogger(PrintStream log) {
        this.logger = log;
        return this;
    }

    public VariableResolver<String> getVariableResolver() {
        return variableResolver;
    }

    public KubernetesClientWrapper withVariableResolver(VariableResolver<String> resolver) {
        this.variableResolver = resolver;
        return this;
    }

    /**
     * Apply Kubernetes configurations through the given Kubernetes client.
     *
     * @param configFiles The configuration files to be deployed
     * @throws IOException          exception on IO
     * @throws InterruptedException interruption happened during blocking IO operations
     */
    public void apply(FilePath[] configFiles) throws IOException, InterruptedException {
        for (FilePath path : configFiles) {
            log(Messages.KubernetesClientWrapper_loadingConfiguration(path));
            List<Object> resources;
            try {
                InputStream inputStream = CommonUtils.replaceMacro(path.read(), variableResolver);
                resources = Yaml.loadAll(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IOException(Messages.KubernetesClientWrapper_invalidYaml(path.getName(), e));
            }
            if (resources.isEmpty()) {
                log(Messages.KubernetesClientWrapper_noResourceLoadedFrom(path));
                continue;
            }

            // Process the Namespace in the list first, as it may be a dependency of other resources.
            Iterator<Object> iterator = resources.iterator();
            while (iterator.hasNext()) {
                Object resource = iterator.next();
                if (resource instanceof V1Namespace) {
                    handleResource(resource);
                    iterator.remove();
                }
            }

            for (Object resource : resources) {
                handleResource(resource);
            }
        }
    }

    /**
     * Get related updater in{@link ResourceUpdaterMap} by resource's class type and handle the resource by updater
     *
     * @param resource k8s resource
     */
    private void handleResource(Object resource) {
        Class<? extends ResourceManager.ResourceUpdater> updaterClass = ResourceUpdaterMap.getUnmodifiableInstance().
                get(resource.getClass());
        if (updaterClass != null) {
            try {
                Constructor constructor = updaterClass.getConstructor(resource.getClass());
                ResourceManager.ResourceUpdater updater = (ResourceManager.ResourceUpdater) constructor
                        .newInstance(resource);
                updater.createOrApply();
            } catch (Exception e) {
                log(Messages.KubernetesClientWrapper_illegalUpdater(resource, e));
            }
        } else {
            log(Messages.KubernetesClientWrapper_skipped(resource));
        }
    }

    /**
     * Construct the dockercfg with all the provided credentials, and create a new Secret resource for the Kubernetes
     * cluster.
     * <p>
     * This can be used by the Pods later to pull images from the private container registry.
     *
     * @param kubernetesNamespace The namespace in which the Secret should be created / updated
     * @param secretName          The name of the Secret
     * @param credentials         All the configured credentials
     * @throws IOException exception on IO
     * @see <a href="https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry">
     * Pull an Image from a Private Registry
     * </a>
     */
    public void createOrReplaceSecrets(
            String kubernetesNamespace,
            String secretName,
            List<ResolvedDockerRegistryEndpoint> credentials) throws IOException {
        log(Messages.KubernetesClientWrapper_prepareSecretsWithName(secretName));

        DockerConfigBuilder dockerConfigBuilder = new DockerConfigBuilder(credentials);
        String dockercfg = dockerConfigBuilder.buildDockercfgBase64();

        Map<String, byte[]> data = new HashMap<>();
        data.put(".dockercfg", dockercfg.getBytes(StandardCharsets.UTF_8));
        V1Secret secret = new V1SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(kubernetesNamespace)
                .endMetadata()
                .withData(data)
                .withType("kubernetes.io/dockercfg")
                .build();
        handleResource(secret);
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    public static String prepareSecretName(String nameCfg, String defaultName, EnvVars envVars) {
        String name = StringUtils.trimToEmpty(envVars.expand(nameCfg));
        if (name.length() > Constants.KUBERNETES_NAME_LENGTH_LIMIT) {
            throw new IllegalArgumentException(Messages.KubernetesClientWrapper_secretNameTooLong(name));
        }

        if (!name.isEmpty()) {
            if (!Constants.KUBERNETES_NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException(Messages.KubernetesClientWrapper_illegalSecretName(name));
            }

            return name;
        }
        // use default name and ensure it conforms the requirements.
        name = defaultName;
        if (StringUtils.isBlank(name)) {
            name = UUID.randomUUID().toString();
        }
        name = Constants.KUBERNETES_SECRET_NAME_PREFIX
                + name.replaceAll("[^0-9a-zA-Z]", "-").toLowerCase();
        if (name.length() > Constants.KUBERNETES_NAME_LENGTH_LIMIT) {
            name = name.substring(0, Constants.KUBERNETES_NAME_LENGTH_LIMIT);
        }
        int suffixLength = Constants.KUBERNETES_NAME_LENGTH_LIMIT - name.length();
        final int randomLength = 8;
        if (suffixLength > randomLength) {
            suffixLength = randomLength;
        }
        String suffix = CommonUtils.randomString(suffixLength, true);
        name += suffix;

        if (name.charAt(name.length() - 1) == '-') {
            name = name.substring(0, name.length() - 1) + 'a';
        }
        return name;
    }

    private void log(String message) {
        if (logger != null) {
            logger.println(message);
        }
    }
}

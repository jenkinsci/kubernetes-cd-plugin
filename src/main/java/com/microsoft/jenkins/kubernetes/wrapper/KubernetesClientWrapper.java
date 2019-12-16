/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.wrapper;

import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.CommonUtils;
import com.microsoft.jenkins.kubernetes.util.Constants;
import com.microsoft.jenkins.kubernetes.util.DockerConfigBuilder;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.util.VariableResolver;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import io.kubesphere.jenkins.kubernetes.generated.KubernetesModelClasses;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KubernetesClientWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientWrapper.class);
    private final ApiClient client;
    private PrintStream logger = System.out;
    private VariableResolver<String> variableResolver;

    private boolean deleteResource;


    private static Map<String, String> apiGroups = new HashMap<>();
    private static List<String> apiVersions = new ArrayList<>();


    private static void initApiGroupMap() {
        apiGroups.put("Admissionregistration", "admissionregistration.k8s.io");
        apiGroups.put("Apiextensions", "apiextensions.k8s.io");
        apiGroups.put("Apiregistration", "apiregistration.k8s.io");
        apiGroups.put("Apps", "apps");
        apiGroups.put("Authentication", "authentication.k8s.io");
        apiGroups.put("Authorization", "authorization.k8s.io");
        apiGroups.put("Autoscaling", "autoscaling");
        apiGroups.put("Extensions", "extensions");
        apiGroups.put("Batch", "batch");
        apiGroups.put("Certificates", "certificates.k8s.io");
        apiGroups.put("Networking", "networking.k8s.io");
        apiGroups.put("Policy", "policy");
        apiGroups.put("RbacAuthorization", "rbac.authorization.k8s.io");
        apiGroups.put("Scheduling", "scheduling.k8s.io");
        apiGroups.put("Settings", "settings.k8s.io");
        apiGroups.put("Storage", "storage.k8s.io");
    }

    private static void initApiVersionList() {
        // Order important
        apiVersions.add("V2beta1");
        apiVersions.add("V2beta2");
        apiVersions.add("V2alpha1");
        apiVersions.add("V1beta2");
        apiVersions.add("V1beta1");
        apiVersions.add("V1alpha1");
        apiVersions.add("V1");
    }

    static {
        try {
            initModelMap();
        } catch (Exception ex) {
            LOGGER.error("Unexpected exception while loading classes: " + ex);
        }
    }

    private static Pair<String, String> getApiGroup(String name) {
        MutablePair<String, String> parts = new MutablePair<>();
        for (Map.Entry<String, String> apiGroup : apiGroups.entrySet()) {
            if (name.startsWith(apiGroup.getKey())) {
                parts.left = apiGroup.getValue();
                parts.right = name.substring(apiGroup.getKey().length());
                break;
            }
        }
        if (parts.left == null) {
            parts.right = name;
        }

        return parts;
    }

    private static Pair<String, String> getApiVersion(String name) {
        MutablePair<String, String> parts = new MutablePair<>();
        for (String version : apiVersions) {
            if (name.startsWith(version)) {
                parts.left = version.toLowerCase();
                parts.right = name.substring(version.length());
                break;
            }
        }
        if (parts.left == null) {
            parts.right = name;
        }

        return parts;
    }

    private static void initModelMap() throws IOException {
        initApiGroupMap();
        initApiVersionList();
        for (Class clazz : KubernetesModelClasses.getAllClasses()) {
            String apiGroupVersion = "";
            String kind = "";
            Pair<String, String> nameParts = getApiGroup(clazz.getSimpleName());
            apiGroupVersion += nameParts.getLeft() == null ? "" : nameParts.getLeft() + "/";

            nameParts = getApiVersion(nameParts.getRight());
            apiGroupVersion += nameParts.getLeft() == null ? "" : nameParts.getLeft();
            kind += nameParts.getRight();
            Yaml.addModelMap(apiGroupVersion, kind, clazz);
        }
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
        KubeConfig config = KubeConfig.loadKubeConfig(new StringReader(kubeConfig));
        try {
            client = Config.fromConfig(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public KubernetesClientWrapper(Reader kubeConfigReader) {
        KubeConfig config = KubeConfig.loadKubeConfig(kubeConfigReader);
        try {
            client = Config.fromConfig(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    }

    public ApiClient getClient() {
        return client;
    }


    public PrintStream getLogger() {
        return logger;
    }

    public boolean isDeleteResource() {
        return deleteResource;
    }

    public KubernetesClientWrapper withDeleteResource(boolean isDeleteResource) {
        this.deleteResource = isDeleteResource;
        return this;
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
    public void apply(FilePath[] configFiles) throws IOException, InterruptedException, ApiException {
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
     * Get related updater in{@link ResourceUpdaterMap} by resource's class type and handle the resource by updater.
     *
     * @param resource k8s resource
     */
    private void handleResource(Object resource) {
        Pair<Class<? extends ResourceManager>,
                Class<? extends ResourceManager.ResourceUpdater>> updaterPair =
                ResourceUpdaterMap.getUnmodifiableInstance().get(resource.getClass());
        ResourceManager.ResourceUpdater updater = null;
        if (updaterPair != null) {
            try {
                Constructor constructor = updaterPair.
                        getRight().getDeclaredConstructor(
                        updaterPair.getLeft(), resource.getClass());
                Constructor resourceManagerConstructor = updaterPair.getLeft()
                        .getConstructor(ApiClient.class);
                ResourceManager resourceManager = (ResourceManager) resourceManagerConstructor.
                        newInstance(getClient());
                resourceManager.setConsoleLogger(getLogger());
                updater = (ResourceManager.ResourceUpdater) constructor
                        .newInstance(resourceManager, resource);

            } catch (Exception e) {
                log(Messages.KubernetesClientWrapper_illegalUpdater(resource, e));
            }

            if (updater != null && !deleteResource) {
                updater.createOrApply();
            } else if (updater != null && deleteResource) {
                updater.delete();
            } else {
                log(Messages.KubernetesClientWrapper_illegalUpdater(resource, null));
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
            List<ResolvedDockerRegistryEndpoint> credentials) throws IOException, ApiException {
        log(Messages.KubernetesClientWrapper_prepareSecretsWithName(secretName));

        DockerConfigBuilder dockerConfigBuilder = new DockerConfigBuilder(credentials);
        String dockercfg = dockerConfigBuilder.buildDockercfgString();

        Map<String, String> data = new HashMap<>();
        data.put(".dockercfg", dockercfg);
        V1Secret secret = new V1SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(kubernetesNamespace)
                .endMetadata()
                .withStringData(data)
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

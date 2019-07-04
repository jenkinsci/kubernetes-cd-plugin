/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.core.EnvironmentInjector;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.kubernetes.KubernetesCDPlugin;
import com.microsoft.jenkins.kubernetes.wrapper.KubernetesClientWrapper;
import com.microsoft.jenkins.kubernetes.Messages;
import com.microsoft.jenkins.kubernetes.credentials.ClientWrapperFactory;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.remoting.ProxyException;
import hudson.util.VariableResolver;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.util.Yaml;
import io.kubesphere.jenkins.kubernetes.generated.KubernetesModelClasses;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * Command to deploy Kubernetes configurations.
 * <p>
 * Mark it as serializable so that the inner Callable can be serialized correctly.
 */
public class DeploymentCommand implements ICommand<DeploymentCommand.IDeploymentCommand>, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentCommand.class);

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
            logger.error("Unexpected exception while loading classes: " + ex);
        }
    }
    private static Pair<String, String> getApiGroup(String name) {
        MutablePair<String, String> parts = new MutablePair<>();
        for (String prefix : apiGroups.keySet()) {
            if (name.startsWith(prefix)) {
                parts.left = apiGroups.get(prefix);
                parts.right = name.substring(prefix.length());
                break;
            }
        }
        if (parts.left == null) parts.right = name;

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
        if (parts.left == null) parts.right = name;

        return parts;
    }

    private static void initModelMap()throws IOException{
        initApiGroupMap();
        initApiVersionList();
        for(Class clazz :KubernetesModelClasses.getAllClasses()){
            String apiGroupVersion = "";
            String kind = "";
            Pair<String, String> nameParts = getApiGroup(clazz.getSimpleName());
            apiGroupVersion += nameParts.getLeft() == null ? "" : nameParts.getLeft() + "/";

            nameParts = getApiVersion(nameParts.getRight());
            apiGroupVersion += nameParts.getLeft() == null ? "" : nameParts.getLeft();
            kind += nameParts.getRight();
            Yaml.addModelMap(apiGroupVersion,kind,clazz);
        }
    }

    @Override
    public void execute(IDeploymentCommand context) {
        JobContext jobContext = context.getJobContext();
        FilePath workspace = jobContext.getWorkspace();
        EnvVars envVars = context.getEnvVars();

        TaskResult taskResult = null;
        try {
            DeploymentTask task = new DeploymentTask();
            task.setWorkspace(workspace);
            task.setTaskListener(jobContext.getTaskListener());
            task.setClientFactory(context.clientFactory(context.getJobContext().getRun().getParent()));
            task.setEnvVars(envVars);
            task.setConfigPaths(context.getConfigs());
            task.setSecretNamespace(context.getSecretNamespace());
            task.setSecretNameCfg(context.getSecretName());
            task.setDefaultSecretNameSeed(jobContext.getRun().getDisplayName());
            task.setEnableSubstitution(context.isEnableConfigSubstitution());
            task.setDockerRegistryEndpoints(context.resolveEndpoints(jobContext.getRun().getParent()));

            taskResult = workspace.act(task);

            for (Map.Entry<String, String> entry : taskResult.extraEnvVars.entrySet()) {
                EnvironmentInjector.inject(jobContext.getRun(), envVars, entry.getKey(), entry.getValue());
            }

            context.setCommandState(taskResult.commandState);
            if (taskResult.commandState.isError()) {
                KubernetesCDPlugin.sendEvent(Constants.AI_KUBERNETES, "DeployFailed",
                        Constants.AI_K8S_MASTER, AppInsightsUtils.hash(taskResult.masterHost));
            } else {
                KubernetesCDPlugin.sendEvent(Constants.AI_KUBERNETES, "Deployed",
                        Constants.AI_K8S_MASTER, AppInsightsUtils.hash(taskResult.masterHost));
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            context.logError(e);
            KubernetesCDPlugin.sendEvent(Constants.AI_KUBERNETES, "DeployFailed",
                    Constants.AI_K8S_MASTER, AppInsightsUtils.hash(taskResult == null ? null : taskResult.masterHost));
        }
    }

    @VisibleForTesting
    static String getMasterHost(KubernetesClientWrapper wrapper) {
        if (wrapper != null) {
            ApiClient client = wrapper.getClient();
            if (client != null) {
                String url = client.getBasePath();
                if (url != null) {
                    return url;
                }
            }
        }
        return "Unknown";
    }

    static class DeploymentTask extends MasterToSlaveCallable<TaskResult, ProxyException> {
        private FilePath workspace;
        private TaskListener taskListener;
        private ClientWrapperFactory clientFactory;
        private EnvVars envVars;

        private String configPaths;
        private String secretNamespace;
        private String secretNameCfg;
        private String defaultSecretNameSeed;
        private boolean enableSubstitution;

        private List<ResolvedDockerRegistryEndpoint> dockerRegistryEndpoints;

        @Override
        public TaskResult call() throws ProxyException {
            try {
                return doCall();
            } catch (Exception ex) {
                // JENKINS-50760
                // JEP-200 restricts the classes allowed to be serialized with XStream to a whitelist.
                // The task being executed in doCall may throw some exceptions from the third party libraries,
                // which will cause SecurityException when it's transferred from the slave back to the master.
                // We catch the exception and wrap the stack trace in a ProxyException which can
                // be serialized properly.
                throw new ProxyException(ex);
            }
        }

        private TaskResult doCall() throws Exception {
            TaskResult result = new TaskResult();

            checkState(StringUtils.isNotBlank(secretNamespace), Messages.DeploymentCommand_blankNamespace());
            checkState(StringUtils.isNotBlank(configPaths), Messages.DeploymentCommand_blankConfigFiles());

            KubernetesClientWrapper wrapper =
                    clientFactory.buildClient(workspace).withLogger(taskListener.getLogger());
            result.masterHost = getMasterHost(wrapper);

            FilePath[] configFiles = workspace.list(configPaths);
            if (configFiles.length == 0) {
                String message = Messages.DeploymentCommand_noMatchingConfigFiles(configPaths);
                taskListener.error(message);
                result.commandState = CommandState.HasError;
                throw new IllegalStateException(message);
            }

            if (!dockerRegistryEndpoints.isEmpty()) {
                String secretName =
                        KubernetesClientWrapper.prepareSecretName(secretNameCfg, defaultSecretNameSeed, envVars);

                wrapper.createOrReplaceSecrets(secretNamespace, secretName, dockerRegistryEndpoints);

                taskListener.getLogger().println(Messages.DeploymentCommand_injectSecretName(
                        Constants.KUBERNETES_SECRET_NAME_PROP, secretName));
                envVars.put(Constants.KUBERNETES_SECRET_NAME_PROP, secretName);
                result.extraEnvVars.put(Constants.KUBERNETES_SECRET_NAME_PROP, secretName);
            }

            if (enableSubstitution) {
                wrapper.withVariableResolver(new VariableResolver.ByMap<>(envVars));
            }

            wrapper.apply(configFiles);

            result.commandState = CommandState.Success;

            return result;
        }

        public void setWorkspace(FilePath workspace) {
            this.workspace = workspace;
        }

        public void setTaskListener(TaskListener taskListener) {
            this.taskListener = taskListener;
        }

        public void setClientFactory(ClientWrapperFactory clientFactory) {
            this.clientFactory = clientFactory;
        }

        public void setEnvVars(EnvVars envVars) {
            this.envVars = envVars;
        }

        public void setConfigPaths(String configPaths) {
            this.configPaths = configPaths;
        }

        public void setSecretNamespace(String secretNamespace) {
            this.secretNamespace = secretNamespace;
        }

        public void setSecretNameCfg(String secretNameCfg) {
            this.secretNameCfg = secretNameCfg;
        }

        public void setDefaultSecretNameSeed(String defaultSecretNameSeed) {
            this.defaultSecretNameSeed = defaultSecretNameSeed;
        }

        public void setEnableSubstitution(boolean enableSubstitution) {
            this.enableSubstitution = enableSubstitution;
        }

        public void setDockerRegistryEndpoints(List<ResolvedDockerRegistryEndpoint> dockerRegistryEndpoints) {
            this.dockerRegistryEndpoints = dockerRegistryEndpoints;
        }
    }

    public static class TaskResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private CommandState commandState = CommandState.Unknown;
        private String masterHost;
        private final Map<String, String> extraEnvVars = new HashMap<>();
    }

    public interface IDeploymentCommand extends IBaseCommandData {
        ClientWrapperFactory clientFactory(Item owner);

        String getSecretNamespace();

        String getSecretName();

        List<ResolvedDockerRegistryEndpoint> resolveEndpoints(Item context) throws IOException;

        String getConfigs();

        boolean isEnableConfigSubstitution();
    }
}

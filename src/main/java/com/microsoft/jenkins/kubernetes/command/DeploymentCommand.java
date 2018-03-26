/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.jenkins.azurecommons.EnvironmentInjector;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.kubernetes.KubernetesCDPlugin;
import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import com.microsoft.jenkins.kubernetes.Messages;
import com.microsoft.jenkins.kubernetes.credentials.ClientWrapperFactory;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.util.VariableResolver;
import io.fabric8.kubernetes.client.KubernetesClient;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
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
    @Override
    public void execute(IDeploymentCommand context) {
        JobContext jobContext = context.getJobContext();

        // all the final variables below are serializable, which will be captured in the below MasterToSlaveCallable
        // and execute on the slave if the job is scheduled on slave.
        final TaskListener taskListener = jobContext.getTaskListener();
        final String secretNameCfg = context.getSecretName();
        final String secretNamespace = context.getSecretNamespace();
        final String configPaths = context.getConfigs();
        final FilePath workspace = jobContext.getWorkspace();
        final String defaultSecretName = jobContext.getRun().getDisplayName();
        final EnvVars envVars = context.getEnvVars();
        final boolean enableSubstitution = context.isEnableConfigSubstitution();
        final ClientWrapperFactory clientFactory = context.clientFactory(context.getJobContext().getRun().getParent());

        TaskResult taskResult = null;
        try {
            final List<ResolvedDockerRegistryEndpoint> dockerRegistryEndpoints =
                    context.resolveEndpoints(jobContext.getRun().getParent());
            taskResult = workspace.act(new MasterToSlaveCallable<TaskResult, Exception>() {
                @Override
                public TaskResult call() throws Exception {
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
                                KubernetesClientWrapper.prepareSecretName(secretNameCfg, defaultSecretName, envVars);

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
            });
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
                    Constants.AI_K8S_MASTER, AppInsightsUtils.hash(taskResult == null ? null : taskResult.masterHost),
                    Constants.AI_MESSAGE, e.getMessage());
        }
    }

    @VisibleForTesting
    String getMasterHost(KubernetesClientWrapper wrapper) {
        if (wrapper != null) {
            KubernetesClient client = wrapper.getClient();
            if (client != null) {
                URL masterURL = client.getMasterUrl();
                if (masterURL != null) {
                    return masterURL.getHost();
                }
            }
        }
        return "Unknown";
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

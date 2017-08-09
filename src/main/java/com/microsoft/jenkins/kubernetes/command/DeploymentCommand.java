/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

import com.microsoft.jenkins.azurecommons.EnvironmentInjector;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.kubernetes.KubernetesCDPlugin;
import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import com.microsoft.jenkins.kubernetes.Messages;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Item;
import hudson.util.VariableResolver;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;

import java.net.URL;
import java.util.List;

public class DeploymentCommand implements ICommand<DeploymentCommand.IDeploymentCommand> {
    @Override
    public void execute(IDeploymentCommand context) {
        JobContext jobContext = context.getJobContext();
        FilePath workspace = jobContext.getWorkspace();
        Item jobItem = jobContext.getRun().getParent();
        EnvVars envVars = jobContext.envVars();
        String kubernetesNamespace = context.getNamespace();

        KubernetesClientWrapper wrapper = null;
        try {
            wrapper = context.buildKubernetesClientWrapper(workspace).withLogger(jobContext.logger());
            if (context.isEnableConfigSubstitution()) {
                wrapper.withVariableResolver(new VariableResolver.ByMap<>(envVars));
            }
            FilePath[] configFiles = workspace.list(context.getConfigs());

            List<DockerRegistryEndpoint> dockerCredentials = context.getDockerCredentials();
            if (!dockerCredentials.isEmpty()) {
                String secretName = KubernetesClientWrapper.prepareSecretName(
                        context.getSecretName(), jobContext.getRun().getDisplayName(), envVars);

                wrapper.createOrReplaceSecrets(jobItem, kubernetesNamespace, secretName, dockerCredentials);

                context.logStatus(Messages.DeploymentCommand_injectSecretName(
                        Constants.KUBERNETES_SECRET_NAME_PROP, secretName));
                EnvironmentInjector.inject(jobContext.getRun(), Constants.KUBERNETES_SECRET_NAME_PROP, secretName);
            }

            wrapper.apply(kubernetesNamespace, configFiles);

            context.setCommandState(CommandState.Success);

            KubernetesCDPlugin.sendEvent(Constants.AI_KUBERNETES, "Deployed",
                    Constants.AI_K8S_MASTER, AppInsightsUtils.hash(getMasterHost(wrapper)));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            context.logError(e);
            KubernetesCDPlugin.sendEvent(Constants.AI_KUBERNETES, "DeployFailed",
                    Constants.AI_K8S_MASTER, AppInsightsUtils.hash(getMasterHost(wrapper)),
                    Constants.AI_MESSAGE, e.getMessage());
        }
    }

    private String getMasterHost(KubernetesClientWrapper wrapper) {
        if (wrapper != null) {
            URL masterURL = wrapper.getClient().getMasterUrl();
            if (masterURL != null) {
                return masterURL.getHost();
            }
        }
        return "Unknown";
    }

    public interface IDeploymentCommand extends IBaseCommandData {
        KubernetesClientWrapper buildKubernetesClientWrapper(FilePath workspace) throws Exception;

        String getNamespace();

        String getSecretName();

        List<DockerRegistryEndpoint> getDockerCredentials();

        String getConfigs();

        boolean isEnableConfigSubstitution();
    }
}

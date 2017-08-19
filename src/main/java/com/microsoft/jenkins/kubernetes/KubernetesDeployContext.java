/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.google.common.collect.ImmutableList;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.command.SimpleBuildStepExecution;
import com.microsoft.jenkins.azurecommons.remote.SSHClient;
import com.microsoft.jenkins.kubernetes.command.DeploymentCommand;
import com.microsoft.jenkins.kubernetes.credentials.ConfigFileCredentials;
import com.microsoft.jenkins.kubernetes.credentials.KubernetesCredentialsType;
import com.microsoft.jenkins.kubernetes.credentials.SSHCredentials;
import com.microsoft.jenkins.kubernetes.credentials.TextCredentials;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class KubernetesDeployContext extends BaseCommandContext implements
        DeploymentCommand.IDeploymentCommand {

    private String credentialsType;
    private SSHCredentials ssh;
    private ConfigFileCredentials kubeConfig;
    private TextCredentials textCredentials;

    private String configs;
    private boolean enableConfigSubstitution;

    private String secretNamespace;
    private String secretName;
    private List<DockerRegistryEndpoint> dockerCredentials;

    @DataBoundConstructor
    public KubernetesDeployContext() {
        enableConfigSubstitution = true;
    }

    public void configure(
            @Nonnull Run<?, ?> run,
            @Nonnull FilePath workspace,
            @Nonnull Launcher launcher,
            @Nonnull TaskListener listener) throws IOException, InterruptedException {

        CommandService commandService = CommandService.builder()
                .withSingleCommand(DeploymentCommand.class)
                .withStartCommand(DeploymentCommand.class)
                .build();

        final JobContext jobContext = new JobContext(run, workspace, launcher, listener);
        super.configure(jobContext, commandService);
    }

    public String getCredentialsType() {
        if (StringUtils.isEmpty(credentialsType)) {
            return KubernetesCredentialsType.DEFAULT.name();
        }
        return credentialsType;
    }

    public KubernetesCredentialsType getCredentialsTypeEnum() {
        return KubernetesCredentialsType.fromString(getCredentialsType());
    }

    @DataBoundSetter
    public void setCredentialsType(String credentialsType) {
        this.credentialsType = StringUtils.trimToEmpty(credentialsType);
    }

    public SSHCredentials getSsh() {
        return ssh;
    }

    @DataBoundSetter
    public void setSsh(SSHCredentials ssh) {
        this.ssh = ssh;
    }

    public ConfigFileCredentials getKubeConfig() {
        return kubeConfig;
    }

    @DataBoundSetter
    public void setKubeConfig(ConfigFileCredentials kubeConfig) {
        this.kubeConfig = kubeConfig;
    }

    public TextCredentials getTextCredentials() {
        return textCredentials;
    }

    @DataBoundSetter
    public void setTextCredentials(TextCredentials textCredentials) {
        this.textCredentials = textCredentials;
    }

    @Override
    public String getSecretNamespace() {
        return StringUtils.isNotBlank(secretNamespace) ? secretNamespace : Constants.DEFAULT_KUBERNETES_NAMESPACE;
    }

    @DataBoundSetter
    public void setSecretNamespace(String secretNamespace) {
        if (Constants.DEFAULT_KUBERNETES_NAMESPACE.equals(secretNamespace)) {
            this.secretNamespace = null;
        } else {
            this.secretNamespace = secretNamespace;
        }
    }

    @Override
    public String getConfigs() {
        return configs;
    }

    @DataBoundSetter
    public void setConfigs(String configs) {
        this.configs = configs;
    }

    @Override
    public String getSecretName() {
        return secretName;
    }

    @DataBoundSetter
    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }

    @Override
    public boolean isEnableConfigSubstitution() {
        return enableConfigSubstitution;
    }

    @DataBoundSetter
    public void setEnableConfigSubstitution(boolean enableConfigSubstitution) {
        this.enableConfigSubstitution = enableConfigSubstitution;
    }

    @Override
    public List<DockerRegistryEndpoint> getDockerCredentials() {
        if (dockerCredentials == null) {
            return ImmutableList.of();
        }
        return dockerCredentials;
    }

    @DataBoundSetter
    public void setDockerCredentials(List<DockerRegistryEndpoint> dockerCredentials) {
        this.dockerCredentials = dockerCredentials;
    }

    public KubernetesClientWrapper buildKubernetesClientWrapper(FilePath workspace) throws Exception {
        switch (getCredentialsTypeEnum()) {
            case SSH:
                FilePath tempConfig = getSsh().getConfigFilePath(workspace);
                try {
                    return new KubernetesClientWrapper(tempConfig.getRemote());
                } finally {
                    tempConfig.delete();
                }
            case KubeConfig:
                return new KubernetesClientWrapper(getKubeConfig().getConfigFilePath(workspace).getRemote());
            case Text:
                TextCredentials text = getTextCredentials();
                return new KubernetesClientWrapper(
                        text.getServerUrl(),
                        text.getCertificateAuthorityData(),
                        text.getClientCertificateData(),
                        text.getClientKeyData());
            default:
                throw new IllegalStateException(
                        Messages.KubernetesDeployContext_unknownCredentialsType(getCredentialsTypeEnum()));
        }
    }

    @Override
    public IBaseCommandData getDataForCommand(ICommand command) {
        return this;
    }

    @Override
    public StepExecution startImpl(StepContext context) throws Exception {
        return new SimpleBuildStepExecution(new KubernetesDeploy(this), context);
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {
        public ListBoxModel doFillCredentialsTypeItems() {
            ListBoxModel model = new ListBoxModel();
            for (KubernetesCredentialsType type : KubernetesCredentialsType.values()) {
                model.add(type.title(), type.name());
            }
            return model;
        }

        public FormValidation doVerifyConfiguration(
                @QueryParameter String credentialsType,
                @QueryParameter("path") String kubeconfigPath,
                @QueryParameter String sshServer,
                @QueryParameter String sshCredentialsId,
                @QueryParameter("serverUrl") String txtServerUrl,
                @QueryParameter("certificateAuthorityData") String txtCertificateAuthorityData,
                @QueryParameter("clientCertificateData") String txtClientCertificateData,
                @QueryParameter("clientKeyData") String txtClientKeyData,
                @QueryParameter String configs) {
            switch (KubernetesCredentialsType.fromString(credentialsType)) {
                case KubeConfig:
                    if (StringUtils.isBlank(kubeconfigPath)) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_kubeconfigNotConfigured()));
                    }
                    break;
                case SSH:
                    if (StringUtils.isBlank(sshServer)) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_sshServerNotConfigured()));
                    }
                    if (StringUtils.isBlank(sshCredentialsId) || Constants.INVALID_OPTION.equals(sshCredentialsId)) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_sshCredentialsNotSelected()));
                    }
                    SSHCredentials sshCredentials = new SSHCredentials();
                    sshCredentials.setSshCredentialsId(StringUtils.trimToEmpty(sshCredentialsId));
                    sshCredentials.setSshServer(StringUtils.trimToEmpty(sshServer));
                    try {
                        SSHClient client = new SSHClient(
                                sshCredentials.getHost(), sshCredentials.getPort(), sshCredentials.getSshCredentials());
                        try (SSHClient connected = client.connect()) {
                            try {
                                connected.execRemote("test -e " + Constants.KUBECONFIG_FILE, false, false);
                            } catch (SSHClient.ExitStatusException e) {
                                return FormValidation.error(Messages.errorMessage(
                                        Messages.KubernetesDeployContext_cannotFindKubeconfigOnServer(
                                                Constants.KUBECONFIG_FILE, sshServer)));
                            }
                        }
                    } catch (Exception e) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_failedOnSSH(e.getMessage())));
                    }
                    break;
                case Text:
                    txtServerUrl = StringUtils.trimToEmpty(txtServerUrl);
                    if (StringUtils.isBlank(txtServerUrl)) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_serverUrlNotConfigured()));
                    }
                    if (!txtServerUrl.startsWith(Constants.HTTPS_PREFIX)) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_serverUrlNotHttps()));
                    }
                    if (StringUtils.isBlank(txtCertificateAuthorityData)) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_certificateAuthorityNotConfigured()));
                    }
                    if (StringUtils.isBlank(txtClientCertificateData)) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_clientCertificateDataNotConfigured()));
                    }
                    if (StringUtils.isBlank(txtClientKeyData)) {
                        return FormValidation.error(Messages.errorMessage(
                                Messages.KubernetesDeployContext_clientKeyDataNotConfigured()));
                    }
                    break;
                default:
                    break;
            }
            if (StringUtils.isBlank(configs)) {
                return FormValidation.error(Messages.errorMessage(
                        Messages.KubernetesDeployContext_configsNotConfigured()));
            }
            return FormValidation.ok(Messages.KubernetesDeployContext_validateSuccess());
        }

        public String getDefaultSecretNamespace() {
            return Constants.DEFAULT_KUBERNETES_NAMESPACE;
        }

        public boolean getDefaultEnableConfigSubstitution() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return SimpleBuildStepExecution.REQUIRED_CONTEXT;
        }

        @Override
        public java.lang.String getFunctionName() {
            return "kubernetesDeploy";
        }

        @Nonnull
        @Override
        public java.lang.String getDisplayName() {
            return Messages.pluginDisplayName();
        }
    }
}

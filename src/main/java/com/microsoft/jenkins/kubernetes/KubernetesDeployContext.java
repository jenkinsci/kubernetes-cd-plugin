/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.microsoft.jenkins.kubernetes.command.DeploymentCommand;
import com.microsoft.jenkins.kubernetes.command.IBaseCommandData;
import com.microsoft.jenkins.kubernetes.command.ICommand;
import com.microsoft.jenkins.kubernetes.command.TransitionInfo;
import com.microsoft.jenkins.kubernetes.credentials.ConfigFileCredentials;
import com.microsoft.jenkins.kubernetes.credentials.KubernetesCredentialsType;
import com.microsoft.jenkins.kubernetes.credentials.SSHCredentials;
import com.microsoft.jenkins.kubernetes.credentials.TextCredentials;
import com.microsoft.jenkins.kubernetes.util.Constants;
import com.microsoft.jenkins.kubernetes.workflow.SimpleBuildStepExecution;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class KubernetesDeployContext extends AbstractBaseContext implements
        DeploymentCommand.IDeploymentCommand {

    private String credentialsType;
    private SSHCredentials ssh;
    private ConfigFileCredentials kubeConfig;
    private TextCredentials textCredentials;

    private String namespace;
    private String configs;
    private boolean enableConfigSubstitution;
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

        Hashtable<Class, TransitionInfo> commands = new Hashtable<>();

        commands.put(DeploymentCommand.class,
                new TransitionInfo(new DeploymentCommand(), null, null));

        final JobContext jobContext = new JobContext(run, workspace, launcher, listener);
        super.configure(jobContext, commands, DeploymentCommand.class);
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
    public String getNamespace() {
        return StringUtils.isNotBlank(namespace) ? namespace : Constants.DEFAULT_KUBERNETES_NAMESPACE;
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        if (Constants.DEFAULT_KUBERNETES_NAMESPACE.equals(namespace)) {
            this.namespace = null;
        } else {
            this.namespace = namespace;
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
                        text.getServer(),
                        text.getCertificateAuthorityData(),
                        text.getClientCertificateData(),
                        text.getClientKeyData());
            default:
                throw new IllegalStateException("Unknown cluster credentials type: " + getCredentialsTypeEnum());
        }
    }

    @Override
    public IBaseCommandData getDataForCommand(ICommand command) {
        return this;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
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

        public FormValidation doVerifyConfiguration(@QueryParameter java.lang.String credentialsType) {
            return FormValidation.ok();
        }

        public String getDefaultNamespace() {
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

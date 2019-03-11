/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.collect.ImmutableList;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.azurecommons.command.CommandService;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.command.SimpleBuildStepExecution;
import com.microsoft.jenkins.azurecommons.remote.SSHClient;
import com.microsoft.jenkins.kubernetes.command.DeploymentCommand;
import com.microsoft.jenkins.kubernetes.command.HelmDeploymentCommand;
import com.microsoft.jenkins.kubernetes.command.HelmRollbackCommand;
import com.microsoft.jenkins.kubernetes.credentials.ClientWrapperFactory;
import com.microsoft.jenkins.kubernetes.credentials.ConfigFileCredentials;
import com.microsoft.jenkins.kubernetes.credentials.KubeconfigCredentials;
import com.microsoft.jenkins.kubernetes.credentials.KubernetesCredentialsType;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.credentials.SSHCredentials;
import com.microsoft.jenkins.kubernetes.credentials.TextCredentials;
import com.microsoft.jenkins.kubernetes.helm.HelmContext;
import com.microsoft.jenkins.kubernetes.helm.HelmRepositoryEndPoint;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class KubernetesDeployContext extends BaseCommandContext implements
        DeploymentCommand.IDeploymentCommand,
        HelmDeploymentCommand.IHelmDeploymentData,
        HelmRollbackCommand.IHelmRollbackData {

    private String kubeconfigId;

    private String credentialsType;
    private SSHCredentials ssh;
    private ConfigFileCredentials kubeConfig;
    private TextCredentials textCredentials;

    private DeployTypeClass deployTypeClass;
    private String deployType;
    private HelmContext helmContext;
    private String helmChartLocation;
    private String helmChartName;
    private String helmChartVersion;
    private String helmReleaseName;
    private String helmNamespace;
    private String tillerNamespace;
    private long helmTimeout;
    private String setValues;
    private boolean helmWait;
    private List<HelmRepositoryEndPoint> helmRepositoryEndPoints;
    private String helmCommandType;
    private String helmChartType;
    private int helmRevisionNumber;
    private String helmRollbackName;

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

        CommandService commandService;
        switch (DeployType.get(getDeployType())) {
            case KUBERNETES:
                commandService = CommandService.builder()
                        .withSingleCommand(DeploymentCommand.class)
                        .withStartCommand(DeploymentCommand.class)
                        .build();
                break;
            case HELM:
                String commandType = getHelmCommandType();
                if (Constants.HELM_COMMAND_TYPE_INSTALL.equals(commandType)) {
                    commandService = CommandService.builder()
                            .withSingleCommand(HelmDeploymentCommand.class)
                            .withStartCommand(HelmDeploymentCommand.class)
                            .build();
                } else if (Constants.HELM_COMMAND_TYPE_ROLLBACK.equals(commandType)) {
                    commandService = CommandService.builder()
                            .withSingleCommand(HelmRollbackCommand.class)
                            .withStartCommand(HelmRollbackCommand.class)
                            .build();
                } else {
                    throw new IOException("Unknown deploy type");
                }
                break;
            default:
                throw new IOException("Unknown deploy type");
        }


        final JobContext jobContext = new JobContext(run, workspace, launcher, listener);
        super.configure(jobContext, commandService);
    }

    public String getKubeconfigId() {
        return kubeconfigId;
    }

    @DataBoundSetter
    public void setKubeconfigId(String kubeconfigId) {
        this.kubeconfigId = kubeconfigId;
    }

    @Deprecated
    public String getCredentialsType() {
        if (StringUtils.isEmpty(credentialsType)) {
            return KubernetesCredentialsType.DEFAULT.name();
        }
        return credentialsType;
    }

    @Deprecated
    public KubernetesCredentialsType getCredentialsTypeEnum() {
        return KubernetesCredentialsType.fromString(getCredentialsType());
    }

    @DataBoundSetter
    @Deprecated
    public void setCredentialsType(String credentialsType) {
        this.credentialsType = StringUtils.trimToEmpty(credentialsType);
    }

    @Deprecated
    public SSHCredentials getSsh() {
        return ssh;
    }

    @Deprecated
    @DataBoundSetter
    public void setSsh(SSHCredentials ssh) {
        this.ssh = ssh;
    }

    @Deprecated
    public ConfigFileCredentials getKubeConfig() {
        return kubeConfig;
    }

    @Deprecated
    @DataBoundSetter
    public void setKubeConfig(ConfigFileCredentials kubeConfig) {
        this.kubeConfig = kubeConfig;
    }

    @Deprecated
    public TextCredentials getTextCredentials() {
        return textCredentials;
    }

    @Deprecated
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

    @DataBoundSetter
    public void setDeployType(String deployType) {
        this.deployType = deployType;
    }

    @DataBoundSetter
    public void setHelmChartType(String helmChartType) {
        this.helmChartType = helmChartType;
    }

    @DataBoundSetter
    public void setDeployTypeClass(DeployTypeClass deployTypeClass) {
        this.deployTypeClass = deployTypeClass;
        this.configs = StringUtils.trimToNull(deployTypeClass.getConfigs());
        HelmCommandClass helmCommandClass = deployTypeClass.getHelmCommandClass();
        this.helmChartLocation = StringUtils.trimToNull(helmCommandClass.getHelmChartLocation());
        this.helmChartName = StringUtils.trimToNull(helmCommandClass.getHelmChartName());
        this.helmChartVersion = StringUtils.trimToNull(helmCommandClass.getHelmChartVersion());
        this.helmReleaseName = StringUtils.trimToNull(helmCommandClass.getHelmReleaseName());
        this.helmNamespace = StringUtils.trimToNull(helmCommandClass.getHelmNamespace());
        this.tillerNamespace = StringUtils.trimToNull(deployTypeClass.getTillerNamespace());
        this.helmWait = deployTypeClass.isHelmWait();
        this.helmTimeout = deployTypeClass.getHelmTimeout();
        this.setValues = helmCommandClass.getSetValues();
        this.helmRepositoryEndPoints = parseHelmRepositoryEndpoint(deployTypeClass.getHelmRepositoryEndPoints());
        this.helmCommandType = deployTypeClass.getHelmCommandType();
        this.helmChartType = helmCommandClass.getHelmChartType();
        this.helmRevisionNumber = helmCommandClass.getHelmRevisionNumber();
        this.helmRollbackName = helmCommandClass.getHelmRollbackName();

        HelmContext.Builder builder = new HelmContext.Builder(getHelmChartLocation())
                .withReleaseName(getHelmReleaseName())
                .withTargetNamespace(getHelmNamespace())
                .withTillerNamespace(getTillerNamespace())
                .withSetValues(getSetValues())
                .withWait(isHelmWait())
                .withHelmRepositoryEndpoints(getHelmRepositoryEndPoints())
                .withHelmCommandType(getHelmCommandType())
                .withHelmChartType(getHelmChartType())
                .withRevisionNumber(getHelmRevisionNumber())
                .withRollbackName(getHelmRollbackName())
                .withChartName(getHelmChartName())
                .withChartVersion(getHelmChartVersion());
        long timeout = getHelmTimeout();
        if (timeout > 0) {
            builder = builder.withTimeout(timeout);
        }
        this.helmContext = builder.build();
    }

    private List<HelmRepositoryEndPoint> parseHelmRepositoryEndpoint(List<HelmRepositoryEndPoint> endPoints) {
        return endPoints;
    }

    public boolean isDeployTypeEquals(String type) {
        if (this.deployType == null && type.equalsIgnoreCase(DeployType.KUBERNETES.getName())) {
            return true;
        }
        return type != null && type.equalsIgnoreCase(this.deployType);
    }

    public boolean isHelmCommandTypeEquals(String type) {
        if (this.helmCommandType == null && type.equalsIgnoreCase(Constants.HELM_COMMAND_TYPE_INSTALL)) {
            return true;
        }
        return type != null && type.equalsIgnoreCase(this.helmCommandType);
    }

    public boolean isHelmChartTypeEquals(String type) {
        if (this.helmChartType == null && type.equalsIgnoreCase(Constants.HELM_CHART_TYPE_URI)) {
            return true;
        }
        return type != null && type.equalsIgnoreCase(this.helmChartType);
    }

    public DeployTypeClass getDeployTypeClass() {
        return deployTypeClass;
    }

    public String getDeployType() {
        return deployType;
    }

    public String getHelmChartLocation() {
        return helmChartLocation;
    }

    public String getHelmChartName() {
        return helmChartName;
    }

    public String getHelmChartVersion() {
        return helmChartVersion;
    }

    public String getHelmReleaseName() {
        return helmReleaseName;
    }

    public String getHelmNamespace() {
        return helmNamespace;
    }

    public String getTillerNamespace() {
        return tillerNamespace;
    }

    public long getHelmTimeout() {
        return helmTimeout;
    }

    public String getSetValues() {
        return setValues;
    }

    public boolean isHelmWait() {
        return helmWait;
    }

    public String getHelmCommandType() {
        return helmCommandType;
    }

    public String getHelmChartType() {
        return helmChartType;
    }

    public int getHelmRevisionNumber() {
        return helmRevisionNumber;
    }

    public String getHelmRollbackName() {
        return helmRollbackName;
    }

    @Override
    public HelmContext getHelmContext() {
        return this.helmContext;
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

    public List<DockerRegistryEndpoint> getDockerCredentials() {
        if (dockerCredentials == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(dockerCredentials);
    }

    @DataBoundSetter
    public void setDockerCredentials(List<DockerRegistryEndpoint> dockerCredentials) {
        List<DockerRegistryEndpoint> endpoints = new ArrayList<>();
        for (DockerRegistryEndpoint endpoint : dockerCredentials) {
            String credentialsId = org.apache.commons.lang.StringUtils.trimToNull(endpoint.getCredentialsId());
            if (credentialsId == null) {
                // no credentials item is selected, skip this endpoint
                continue;
            }

            String registryUrl = org.apache.commons.lang.StringUtils.trimToNull(endpoint.getUrl());
            // null URL results in "https://index.docker.io/v1/" effectively
            if (registryUrl != null) {
                // It's common that the user omits the scheme prefix, we add http:// as default.
                // Otherwise it will cause MalformedURLException when we call endpoint.getEffectiveURL();
                if (!Constants.URI_SCHEME_PREFIX.matcher(registryUrl).find()) {
                    registryUrl = "http://" + registryUrl;
                }
            }
            endpoints.add(new DockerRegistryEndpoint(registryUrl, credentialsId));
        }
        this.dockerCredentials = endpoints;
    }

    @Override
    public List<HelmRepositoryEndPoint> getHelmRepositoryEndPoints() {
        if (this.helmRepositoryEndPoints == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(this.helmRepositoryEndPoints);
    }

    @Override
    public List<ResolvedDockerRegistryEndpoint> resolveEndpoints(Item context) throws IOException {
        List<ResolvedDockerRegistryEndpoint> endpoints = new ArrayList<>();
        List<DockerRegistryEndpoint> configured = getDockerCredentials();
        for (DockerRegistryEndpoint endpoint : configured) {
            DockerRegistryToken token = endpoint.getToken(context);
            if (token == null) {
                throw new IllegalArgumentException("No credentials found for " + endpoint);
            }
            endpoints.add(new ResolvedDockerRegistryEndpoint(endpoint.getEffectiveUrl(), token));
        }
        return endpoints;
    }

    @Override
    public ClientWrapperFactory clientFactory(Item owner) {
        final String configId = getKubeconfigId();
        if (StringUtils.isNotBlank(configId)) {
            final KubeconfigCredentials credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            KubeconfigCredentials.class,
                            owner,
                            ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList()),
                    CredentialsMatchers.withId(configId));
            if (credentials == null) {
                throw new IllegalArgumentException("Cannot find kubeconfig credentials with id " + configId);
            }
            credentials.bindToAncestor(owner);
            return new ClientWrapperFactoryImpl(credentials);
        }

        // Fallback to the legacy handling
        switch (getCredentialsTypeEnum()) {
            case SSH:
                return getSsh().buildClientWrapperFactory(owner);
            case KubeConfig:
                return getKubeConfig().buildClientWrapperFactory(owner);
            case Text:
                return getTextCredentials().buildClientWrapperFactory(owner);
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
        public String doFillDeployTypeItems() {
            return null;
        }

        public String doFillHelmCommandTypeItems() {
            return null;
        }

        public ListBoxModel doFillCredentialsTypeItems() {
            ListBoxModel model = new ListBoxModel();
            for (KubernetesCredentialsType type : KubernetesCredentialsType.values()) {
                model.add(type.title(), type.name());
            }
            return model;
        }

        public ListBoxModel doFillKubeconfigIdItems(@AncestorInPath Item owner) {
            StandardListBoxModel model = new StandardListBoxModel();
            model.includeEmptyValue();
            model.includeAs(ACL.SYSTEM, owner, KubeconfigCredentials.class);
            return model;
        }

        public FormValidation doVerifyConfiguration(
                @AncestorInPath Item owner,
                @QueryParameter("kubeconfigId") String configId,
                @QueryParameter String credentialsType,
                @QueryParameter("path") String kubeconfigPath,
                @QueryParameter String sshServer,
                @QueryParameter String sshCredentialsId,
                @QueryParameter("serverUrl") String txtServerUrl,
                @QueryParameter("certificateAuthorityData") String txtCertificateAuthorityData,
                @QueryParameter("clientCertificateData") String txtClientCertificateData,
                @QueryParameter("clientKeyData") String txtClientKeyData,
                @QueryParameter String configs) {
            try {
                return verifyConfigurationInternal(owner,
                        configId,
                        credentialsType,
                        kubeconfigPath,
                        sshServer, sshCredentialsId,
                        txtServerUrl, txtCertificateAuthorityData, txtClientCertificateData, txtClientKeyData,
                        configs);
            } catch (Exception ex) {
                return FormValidation.error(ex.getMessage());
            }
        }

        private FormValidation verifyConfigurationInternal(Item owner,
                                                           String configId,
                                                           String credentialsType,
                                                           String kubeconfigPath,
                                                           String sshServer,
                                                           String sshCredentialsId,
                                                           String txtServerUrl,
                                                           String txtCertificateAuthorityData,
                                                           String txtClientCertificateData,
                                                           String txtClientKeyData,
                                                           String configs) {
            if (StringUtils.isNotBlank(configId)) {
                final KubeconfigCredentials credentials = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(
                                KubeconfigCredentials.class,
                                owner,
                                ACL.SYSTEM,
                                Collections.<DomainRequirement>emptyList()),
                        CredentialsMatchers.withId(configId));
                if (credentials == null) {
                    return FormValidation.error(
                            Messages.KubernetesDeployContext_kubeconfigCredentialsNotFound(configId));
                }
                credentials.bindToAncestor(owner);
                String content = credentials.getContent();
                if (StringUtils.isBlank(content)) {
                    return FormValidation.error(Messages.KubernetesDeployContext_noKubeconfigContent());
                }
            } else {
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
                        if (StringUtils.isBlank(sshCredentialsId)
                                || Constants.INVALID_OPTION.equals(sshCredentialsId)) {
                            return FormValidation.error(Messages.errorMessage(
                                    Messages.KubernetesDeployContext_sshCredentialsNotSelected()));
                        }
                        SSHCredentials sshCredentials = new SSHCredentials();
                        sshCredentials.setSshCredentialsId(StringUtils.trimToEmpty(sshCredentialsId));
                        sshCredentials.setSshServer(StringUtils.trimToEmpty(sshServer));
                        try {
                            SSHClient client = new SSHClient(
                                    sshCredentials.getHost(),
                                    sshCredentials.getPort(),
                                    sshCredentials.getSshCredentials(owner));
                            try (SSHClient connected = client.connect()) {
                                try {
                                    connected.execRemote(
                                            "test -e " + Constants.KUBECONFIG_FILE, false, false);
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

        public boolean getDefaultHelmWait() {
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

    private static class ClientWrapperFactoryImpl implements ClientWrapperFactory {
        private final KubeconfigCredentials kubeconfig;

        ClientWrapperFactoryImpl(KubeconfigCredentials kubeconfig) {
            this.kubeconfig = kubeconfig;
        }

        @Override
        public KubernetesClientWrapper buildClient(FilePath workspace) {
            return new KubernetesClientWrapper(kubeconfig.getContent());
        }
    }

    public static class HelmCommandClass {
        private String helmChartLocation;
        private String helmChartName;
        private String helmChartVersion;
        private String helmReleaseName;
        private String helmNamespace;
        private String helmChartType;
        private String helmRollbackName;
        private int helmRevisionNumber;
        private String setValues;

        @DataBoundConstructor
        public HelmCommandClass(String helmChartLocation,
                                String helmChartName,
                                String helmChartVersion,
                                String helmReleaseName,
                                String helmNamespace,
                                String helmChartType,
                                String helmRollbackName,
                                int helmRevisionNumber,
                                String setValues) {
            this.helmChartLocation = helmChartLocation;
            this.helmChartName = helmChartName;
            this.helmChartVersion = helmChartVersion;
            this.helmReleaseName = helmReleaseName;
            this.helmNamespace = helmNamespace;
            this.helmChartType = helmChartType;
            this.helmRollbackName = helmRollbackName;
            this.helmRevisionNumber = helmRevisionNumber;
            this.setValues = setValues;
        }

        public String getHelmChartLocation() {
            return helmChartLocation;
        }

        public String getHelmChartName() {
            return helmChartName;
        }

        public String getHelmChartVersion() {
            return helmChartVersion;
        }

        public String getHelmReleaseName() {
            return helmReleaseName;
        }

        public String getHelmNamespace() {
            return helmNamespace;
        }

        public String getHelmChartType() {
            return helmChartType;
        }

        public String getHelmRollbackName() {
            return helmRollbackName;
        }

        public int getHelmRevisionNumber() {
            return helmRevisionNumber;
        }

        public String getSetValues() {
            return setValues;
        }
    }

    /**
     * Class Reflected with deployment configurations.
     */
    public static class DeployTypeClass {
        private String configs;
        private String tillerNamespace;
        private long helmTimeout;
        private boolean helmWait;
        private List<HelmRepositoryEndPoint> helmRepositoryEndPoints;
        private String helmCommandType;
        private HelmCommandClass helmCommandClass;

        @DataBoundConstructor
        public DeployTypeClass(String configs,
                               String tillerNamespace,
                               long helmTimeout,
                               boolean helmWait,
                               List<HelmRepositoryEndPoint> helmRepositoryEndPoints,
                               String helmCommandType,
                               HelmCommandClass helmCommandClass) {
            this.configs = configs;
            this.helmCommandClass = helmCommandClass;
            this.tillerNamespace = tillerNamespace;
            this.helmTimeout = helmTimeout;
            this.helmWait = helmWait;
            this.helmRepositoryEndPoints = helmRepositoryEndPoints;
            this.helmCommandType = helmCommandType;
        }

        public String getConfigs() {
            return configs;
        }

        public String getTillerNamespace() {
            return tillerNamespace;
        }

        public long getHelmTimeout() {
            return helmTimeout;
        }

        public boolean isHelmWait() {
            return helmWait;
        }

        public List<HelmRepositoryEndPoint> getHelmRepositoryEndPoints() {
            return helmRepositoryEndPoints;
        }

        public HelmCommandClass getHelmCommandClass() {
            return helmCommandClass;
        }

        public String getHelmCommandType() {
            return helmCommandType;
        }

    }
}

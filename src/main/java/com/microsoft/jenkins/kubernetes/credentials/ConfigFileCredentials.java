/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import com.microsoft.jenkins.kubernetes.Messages;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * @deprecated Use {@link KubeconfigCredentials}.
 */
@Deprecated
public class ConfigFileCredentials
        extends AbstractDescribableImpl<ConfigFileCredentials>
        implements ClientWrapperFactory.Builder {

    private String path;

    @DataBoundConstructor
    public ConfigFileCredentials() {
    }

    public String getPath() {
        return path;
    }

    @DataBoundSetter
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public ClientWrapperFactory buildClientWrapperFactory() {
        return new ClientWrapperFactoryImpl(getPath());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<ConfigFileCredentials> {
        public FormValidation doCheckPath(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.ConfigFileCredentials_pathRequired());
            }
            return FormValidation.ok();
        }
    }

    private static class ClientWrapperFactoryImpl implements ClientWrapperFactory {
        private static final long serialVersionUID = 1L;

        private final String configFilePath;

        ClientWrapperFactoryImpl(String configFilePath) {
            this.configFilePath = configFilePath;
        }

        @Override
        public KubernetesClientWrapper buildClient(FilePath workspace) throws Exception {
            FilePath configFile = workspace.child(configFilePath);
            if (!configFile.exists()) {
                throw new IllegalArgumentException(
                        Messages.ConfigFileCredentials_configFileNotFound(configFilePath, workspace));
            }
            return new KubernetesClientWrapper(configFile.getRemote());
        }
    }
}

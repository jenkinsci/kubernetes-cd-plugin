/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

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

public class ConfigFileCredentials
        extends AbstractDescribableImpl<ConfigFileCredentials>
        implements ConfigFileProvider {
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
    public FilePath getConfigFilePath(FilePath workspace) {
        return workspace.child(path);
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
}

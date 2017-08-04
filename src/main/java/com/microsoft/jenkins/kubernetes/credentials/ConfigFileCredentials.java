/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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
    }
}

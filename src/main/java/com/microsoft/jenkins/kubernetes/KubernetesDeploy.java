/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class KubernetesDeploy extends Builder implements SimpleBuildStep {
    private final KubernetesDeployContext context;

    @DataBoundConstructor
    public KubernetesDeploy(KubernetesDeployContext context) {
        this.context = context;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {

        listener.getLogger().println(Messages.KubernetesDeploy_starting());
        this.context.configure(run, workspace, launcher, listener);
        this.context.executeCommands();

        if (context.getLastCommandState().isError()) {
            run.setResult(Result.FAILURE);
            throw new AbortException(Messages.KubernetesDeploy_endWithErrorState(context.getCommandState()));
        } else {
            listener.getLogger().println(Messages.KubernetesDeploy_finished());
        }
    }

    public KubernetesDeployContext getContext() {
        return context;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.pluginDisplayName();
        }
    }
}

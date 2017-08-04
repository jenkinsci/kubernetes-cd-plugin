/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.google.common.collect.ImmutableMap;
import com.microsoft.jenkins.kubernetes.command.DeploymentState;
import com.microsoft.jenkins.kubernetes.command.IBaseCommandData;
import com.microsoft.jenkins.kubernetes.command.ICommand;
import com.microsoft.jenkins.kubernetes.command.TransitionInfo;
import com.microsoft.jenkins.kubernetes.service.ICommandServiceData;
import org.jenkinsci.plugins.workflow.steps.Step;

import java.util.Map;

public abstract class AbstractBaseContext extends Step implements ICommandServiceData {
    private transient JobContext jobContext;
    private transient DeploymentState deployState = DeploymentState.Unknown;
    private transient ImmutableMap<Class, TransitionInfo> commands;
    private transient Class startCommandClass;

    protected void configure(JobContext jobCtx, Map<Class, TransitionInfo> cmds, Class startCmdClass) {
        this.jobContext = jobCtx;
        this.commands = ImmutableMap.copyOf(cmds);
        this.startCommandClass = startCmdClass;
    }

    @Override
    public Map<Class, TransitionInfo> getCommands() {
        return commands;
    }

    @Override
    public Class getStartCommandClass() {
        return startCommandClass;
    }

    @Override
    public abstract IBaseCommandData getDataForCommand(ICommand command);

    public void setDeploymentState(DeploymentState state) {
        this.deployState = state;
    }

    public DeploymentState getDeploymentState() {
        return this.deployState;
    }

    public boolean getHasError() {
        return this.deployState.equals(DeploymentState.HasError);
    }

    public boolean getIsFinished() {
        return this.deployState.equals(DeploymentState.HasError)
                || this.deployState.equals(DeploymentState.Done);
    }

    public final JobContext jobContext() {
        return jobContext;
    }

    public void logStatus(String status) {
        jobContext().getTaskListener().getLogger().println(status);
    }

    public void logError(Exception ex) {
        this.logError(Messages.errorPrefix(), ex);
    }

    public void logError(String prefix, Exception ex) {
        ex.printStackTrace(jobContext().getTaskListener().error(prefix + ex.getMessage()));
        setDeploymentState(DeploymentState.HasError);
    }

    public void logError(String message) {
        jobContext().getTaskListener().error(message);
        setDeploymentState(DeploymentState.HasError);
    }
}

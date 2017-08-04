/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.microsoft.jenkins.kubernetes.util.CommonUtils;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Encapsulates the context for a Jenkins build job.
 */
public class JobContext {
    private final Run<?, ?> run;
    private final FilePath workspace;
    private final Launcher launcher;
    private final TaskListener taskListener;

    public JobContext(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener taskListener) {
        this.run = run;
        this.workspace = workspace;
        this.launcher = launcher;
        this.taskListener = taskListener;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public TaskListener getTaskListener() {
        return taskListener;
    }

    public EnvVars envVars() {
        try {
            return getRun().getEnvironment(getTaskListener());
        } catch (IOException e) {
            throw new RuntimeException(Messages.JobContext_failedToGetEnv(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(Messages.JobContext_failedToGetEnv(), e);
        }
    }

    public PrintStream logger() {
        return getTaskListener().getLogger();
    }

    public FilePath remoteWorkspacePath() {
        return new FilePath(launcher.getChannel(), workspace.getRemote());
    }

    public InputStream replaceMacro(InputStream original, boolean enabled) throws IOException {
        if (enabled) {
            return CommonUtils.replaceMacro(original, new VariableResolver.ByMap<>(envVars()));
        } else {
            return original;
        }
    }
}

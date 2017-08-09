package com.microsoft.jenkins.kubernetes.command;

import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.kubernetes.KubernetesClientWrapper;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.VariableResolver;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.PrintStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DeploymentCommand}
 */
public class DeploymentCommandTest {
    @Test
    public void testNormal() throws Exception {
        ContextBuilder context = new ContextBuilder();
        context.executeCommand();

        verify(context.wrapper, times(1)).withVariableResolver(any(VariableResolver.class));
        verify(context.wrapper, times(1)).createOrReplaceSecrets(context.job, context.kubernetesNamespace, context.secretName, context.dockerCredentials);
        verify(context.wrapper, times(1)).apply(context.kubernetesNamespace, context.configFiles);
        verify(context.context, times(1)).setCommandState(CommandState.Success);
    }

    @Test
    public void testDisableConfigSubstitution() throws Exception {
        ContextBuilder context = new ContextBuilder().disableConfigSubstitution();
        context.executeCommand();

        verify(context.wrapper, never()).withVariableResolver(any(VariableResolver.class));
    }

    @Test
    public void testWithoutDockerCredentials() throws Exception {
        ContextBuilder context = new ContextBuilder().withoutDockerCredentials();
        context.executeCommand();

        verify(context.wrapper, never()).createOrReplaceSecrets(any(Job.class), any(String.class), any(String.class), any(List.class));
    }

    @Test
    public void testExceptionHandling() throws Exception {
        ContextBuilder context = new ContextBuilder().withApplyException();
        context.executeCommand();

        verify(context.context, never()).setCommandState(CommandState.Success);
        verify(context.context, times(1)).logError(context.exception);
    }

    private static class ContextBuilder {
        DeploymentCommand.IDeploymentCommand context;

        JobContext jobContext;
        FilePath workspace;
        Job job;
        EnvVars envVars;
        String kubernetesNamespace;

        KubernetesClientWrapper wrapper;

        boolean enableConfigSubstitution;

        FilePath[] configFiles;
        List<DockerRegistryEndpoint> dockerCredentials;

        String secretName;

        CommandState commandState = null;

        Exception exception;

        ContextBuilder() throws Exception {
            context = mock(DeploymentCommand.IDeploymentCommand.class);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    commandState = invocation.getArgument(0);
                    return null;
                }
            }).when(context).setCommandState(any(CommandState.class));

            jobContext = mock(JobContext.class);
            workspace = mock(FilePath.class);
            job = mock(Job.class);
            envVars = mock(EnvVars.class);

            doReturn(jobContext).when(context).getJobContext();
            doReturn(workspace).when(jobContext).getWorkspace();
            Run run = mock(Run.class);
            doReturn(job).when(run).getParent();
            doReturn(run).when(jobContext).getRun();
            doReturn(envVars).when(jobContext).envVars();

            kubernetesNamespace = "default";
            doReturn(kubernetesNamespace).when(context).getNamespace();

            PrintStream logger = mock(PrintStream.class);
            doReturn(logger).when(jobContext).logger();
            wrapper = mock(KubernetesClientWrapper.class);
            doReturn(wrapper).when(context).buildKubernetesClientWrapper(workspace);
            doReturn(wrapper).when(wrapper).withLogger(logger);
            doReturn(wrapper).when(wrapper).withVariableResolver(any(VariableResolver.class));

            enableConfigSubstitution = true;
            doReturn(enableConfigSubstitution).when(context).isEnableConfigSubstitution();

            configFiles = new FilePath[1];
            doReturn("*").when(context).getConfigs();
            doReturn(configFiles).when(workspace).list("*");

            //
            dockerCredentials = mock(List.class);
            doReturn(false).when(dockerCredentials).isEmpty();
            doReturn(dockerCredentials).when(context).getDockerCredentials();

            // secret name processing.
            secretName = "secret";
            doReturn(secretName).when(context).getSecretName();
            doAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return (String) invocationOnMock.getArgument(0);
                }
            }).when(envVars).expand(any(String.class));
        }

        public ContextBuilder disableConfigSubstitution() {
            enableConfigSubstitution = false;
            doReturn(enableConfigSubstitution).when(context).isEnableConfigSubstitution();
            return this;
        }

        public ContextBuilder withoutDockerCredentials() {
            doReturn(true).when(dockerCredentials).isEmpty();
            return this;
        }

        public ContextBuilder withApplyException() throws Exception {
            exception = new RuntimeException("ApplyException");
            doThrow(exception).when(wrapper).apply(any(String.class), any(FilePath[].class));
            return this;
        }

        void executeCommand() {
            DeploymentCommand command = spy(new DeploymentCommand());
            doReturn("test").when(command).getMasterHost(any(KubernetesClientWrapper.class));
            command.execute(context);
        }
    }
}

package com.microsoft.jenkins.kubernetes.command;

import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.kubernetes.KubernetesDeployContext;
import com.microsoft.jenkins.kubernetes.helm.HelmContext;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hapi.services.tiller.Tiller;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.microbean.helm.ReleaseManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HelmRollbackCommand.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.security.auth.x500.X500Principal"})
public class HelmRollbackCommandTest {

    @Test
    public void testRollback() throws Exception {

        HelmContext helmContext = mock(HelmContext.class);
        when(helmContext.getReleaseName()).thenReturn("releaseName");
        Future<Tiller.RollbackReleaseResponse> rollback = mock(Future.class);
        ReleaseManager releaseManager = mock(ReleaseManager.class);
        doReturn(rollback).when(releaseManager).rollback(nullable(Tiller.RollbackReleaseRequest.class));
        when(releaseManager.list(any())).thenReturn(Collections.emptyIterator());

        HelmRollbackCommand command = PowerMockito.spy(new HelmRollbackCommand());
        doReturn(releaseManager).when(command).getReleaseManager(nullable(DefaultKubernetesClient.class), nullable(String.class));
        doReturn("kubeconfig").when(command).getKubeConfigContent(anyString(), notNull());

        when(helmContext.getHelmCommandType()).thenReturn(Constants.HELM_COMMAND_TYPE_ROLLBACK);
        when(helmContext.getRollbackName()).thenReturn("testrollback");
        when(helmContext.getRevisionNumber()).thenReturn(1);

        KubernetesDeployContext context = spy(KubernetesDeployContext.class);
        when(context.getHelmContext()).thenReturn(helmContext);
        when(context.getKubeconfigId()).thenReturn("kubeconfigid");
        FreeStyleProject project = mock(FreeStyleProject.class);
        FilePath workspace = new FilePath(new File(System.getProperty("user.dir")));
        JobContext jobContext = mock(JobContext.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getJobContext().getWorkspace()).thenReturn(workspace);
        when(context.getHelmChartLocation()).thenReturn(".");
        when(context.getJobContext().getOwner()).thenReturn(project);

        command.execute(context);

        verify(releaseManager, times(1)).rollback(any());
        Assert.assertEquals(CommandState.Success, context.getCommandState());
    }
}

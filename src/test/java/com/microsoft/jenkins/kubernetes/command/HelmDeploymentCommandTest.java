package com.microsoft.jenkins.kubernetes.command;

import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.kubernetes.KubernetesDeployContext;
import com.microsoft.jenkins.kubernetes.helm.HelmContext;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hapi.chart.ChartOuterClass;
import hapi.services.tiller.Tiller;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.microbean.helm.ReleaseManager;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HelmDeploymentCommand.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.security.auth.x500.X500Principal"})
@Ignore
public class HelmDeploymentCommandTest {
    private HelmContext helmContext = mock(HelmContext.class);
    private HelmDeploymentCommand command = PowerMockito.spy(new HelmDeploymentCommand());
    private ReleaseManager releaseManager = mock(ReleaseManager.class);
    private KubernetesDeployContext context = Mockito.spy(KubernetesDeployContext.class);

    @Before
    public void init() throws Exception {
        when(helmContext.getReleaseName()).thenReturn("releaseName");



        doReturn(releaseManager).when(command).getReleaseManager(nullable(String.class), nullable(String.class));
        doReturn("kubeconfig").when(command).getKubeConfigContent(anyString(), notNull());
        doReturn(Collections.emptyIterator()).when(releaseManager).list(nullable(Tiller.ListReleasesRequest.class));
        when(helmContext.getHelmChartType()).thenReturn(Constants.HELM_CHART_TYPE_URI);

        when(helmContext.getChartLocation()).thenReturn(".");
        when(helmContext.getTargetNamespace()).thenReturn("targetNamespace");
        when(helmContext.getTimeout()).thenReturn(100L);
        when(helmContext.isWait()).thenReturn(true);

        when(context.getHelmContext()).thenReturn(helmContext);
        when(context.getKubeconfigId()).thenReturn("kubeconfigid");
        FreeStyleProject project = mock(FreeStyleProject.class);
        FilePath workspace = new FilePath(new File(System.getProperty("user.dir")));
        JobContext jobContext = mock(JobContext.class);
        when(context.getJobContext()).thenReturn(jobContext);
        when(context.getJobContext().getWorkspace()).thenReturn(workspace);
        when(context.getHelmChartLocation()).thenReturn(".");
        when(context.getJobContext().getOwner()).thenReturn(project);
        when(context.getJobContext().logger()).thenReturn(System.out);
    }

    @Test
    public void testHelmInstall() throws Exception {
        Future<Tiller.InstallReleaseResponse> install = mock(Future.class);
        doReturn(install).when(releaseManager).install(nullable(Tiller.InstallReleaseRequest.Builder.class), nullable(ChartOuterClass.Chart.Builder.class));


        doReturn(false).when(command, "isHelmReleaseExist", any(), any());
        when(helmContext.getHelmCommandType()).thenReturn(Constants.HELM_COMMAND_TYPE_INSTALL);

        command.execute(context);

        verify(releaseManager, times(1)).install(any(), any());
        Assert.assertEquals(CommandState.Success, context.getCommandState());
    }

    @Test
    public void testHelmUpdate() throws Exception {
        Future<Tiller.UpdateReleaseRequest> update = mock(Future.class);
        doReturn(update).when(releaseManager).update(nullable(Tiller.UpdateReleaseRequest.Builder.class), nullable(ChartOuterClass.Chart.Builder.class));

        doReturn(true).when(command, "isHelmReleaseExist", any(), any());
        when(helmContext.getHelmCommandType()).thenReturn(Constants.HELM_COMMAND_TYPE_INSTALL);

        command.execute(context);

        verify(releaseManager, times(1)).update(any(), any());
        Assert.assertEquals(CommandState.Success, context.getCommandState());
    }
}

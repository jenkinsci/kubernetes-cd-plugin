/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.kubernetes.helm.HelmContext;
import com.microsoft.jenkins.kubernetes.helm.HelmRepositoryEndPoint;
import com.microsoft.jenkins.kubernetes.util.BasicAuthenticator;
import com.microsoft.jenkins.kubernetes.util.Constants;
import hapi.chart.ChartOuterClass;
import hapi.release.ReleaseOuterClass;
import hapi.release.StatusOuterClass;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;
import hapi.services.tiller.Tiller.ListReleasesRequest;
import hapi.services.tiller.Tiller.ListReleasesResponse;
import hapi.services.tiller.Tiller.UpdateReleaseRequest;
import hapi.services.tiller.Tiller.UpdateReleaseResponse;
import hudson.security.ACL;
import io.codearte.props2yaml.Props2YAML;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.chart.DirectoryChartLoader;
import org.microbean.helm.chart.repository.ChartRepository;
import org.microbean.helm.chart.resolver.ChartResolverException;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class HelmDeploymentCommand extends HelmCommand
        implements ICommand<HelmDeploymentCommand.IHelmDeploymentData> {
//    private static Logger LOGGER = Logger.getLogger(HelmDeploymentCommand.class.getName());

    @Override
    public void execute(IHelmDeploymentData context) {
        HelmContext helmContext = context.getHelmContext();
        String tillerNamespace = helmContext.getTillerNamespace();

        String kubeconfigId = context.getKubeconfigId();
        if (StringUtils.isBlank(kubeconfigId)) {
            throw new IllegalArgumentException("Kubeconfig id is empty, please check your configuration");
        }
        String kubeConfig = getKubeConfigContent(kubeconfigId, context.getJobContext().getOwner());

        try (final DefaultKubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(kubeConfig));
             final ReleaseManager releaseManager = getReleaseManager(client, tillerNamespace)) {
            String chartType = helmContext.getHelmChartType();
            ChartOuterClass.Chart.Builder chart = null;
            switch (chartType) {
                case Constants.HELM_CHART_TYPE_URI:
                    String helmChartLocation = helmContext.getChartLocation();
                    helmChartLocation = context.getJobContext().getWorkspace()
                            .child(helmChartLocation).getRemote();

                    File file = new File(helmChartLocation);
                    if (!file.exists()) {
                        throw new IOException(String.format("cannot find helm chart at %s", file.getAbsolutePath()));
                    }
                    URI uri = file.toURI();
                    try (final DirectoryChartLoader chartLoader = new DirectoryChartLoader()) {
                        Path path = Paths.get(uri);
                        chart = chartLoader.load(path);
                    } catch (IOException e) {
                        throw new IOException(String.format("Fail to load helm chart from %s", uri.toString()));
                    }
                    context.logStatus(String.format("Load chart from %s", helmChartLocation));
                    break;

                case Constants.HELM_CHART_TYPE_REPOSITORY:
                    List<HelmRepositoryEndPoint> helmRepositoryEndPoints = context.getHelmRepositoryEndPoints();
                    if (CollectionUtils.isNotEmpty(helmRepositoryEndPoints)) {
                        String chartName = helmContext.getChartName();
                        String chartVersion = helmContext.getChartVersion();
                        for (HelmRepositoryEndPoint helmRepositoryEndPoint : helmRepositoryEndPoints) {
                            String credentialsId = helmRepositoryEndPoint.getCredentialsId();
                            StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(
                                    CredentialsProvider.lookupCredentials(
                                            StandardUsernamePasswordCredentials.class,
                                            Jenkins.getInstance(),
                                            ACL.SYSTEM,
                                            Collections.emptyList()),
                                    CredentialsMatchers.withId(credentialsId));
                            if (credentials != null) {
                                Authenticator.setDefault(new BasicAuthenticator(credentials.getUsername(),
                                        credentials.getPassword().getPlainText()));
                            }

                            ChartRepository chartRepository =
                                    new ChartRepository(helmRepositoryEndPoint.getName(),
                                            URI.create(helmRepositoryEndPoint.getUrl()));
                            try {
                                chart = chartRepository.resolve(chartName, chartVersion);
                            } catch (ChartResolverException e) {
                                context.logError(String.format("Failed to resolve chart %s:%s from %s",
                                        chartName, chartVersion, helmRepositoryEndPoint.toString()));
                            }
                        }
                        if (chart == null) {
                            throw new IOException(String.format("Failed to resolve chart "
                                    + "%s:%s, please check your configuration", chartName, chartVersion));
                        }
                    }
                    break;
                default:
                    throw new IOException(String.format("Do not support chart type %s", chartType));
            }

            createOrUpdateHelm(releaseManager, helmContext, chart);

            context.setCommandState(CommandState.Success);
        } catch (IOException e) {
            context.logError(e);
            context.setCommandState(CommandState.HasError);
        }
    }

    private void createOrUpdateHelm(ReleaseManager releaseManager, HelmContext helmContext,
                                    ChartOuterClass.Chart.Builder chart) throws IOException {
        if (isHelmReleaseExist(releaseManager, helmContext)) {
            updateHelmRelease(releaseManager, helmContext, chart);
        } else {
            installHelmRelease(releaseManager, helmContext, chart);
        }
    }

    private boolean isHelmReleaseExist(ReleaseManager releaseManager, HelmContext helmContext) throws IOException {
        ListReleasesRequest.Builder builder = ListReleasesRequest.newBuilder();
        builder.setFilter(helmContext.getReleaseName());
//        builder.addStatusCodes(StatusOuterClass.Status.Code.DEPLOYED);
        builder.addAllStatusCodes(
                Arrays.asList(StatusOuterClass.Status.Code.DEPLOYED, StatusOuterClass.Status.Code.FAILED)
        );
        Iterator<ListReleasesResponse> responses = releaseManager.list(builder.build());
        if (responses.hasNext()) {
            ListReleasesResponse releasesResponse = responses.next();
            List<ReleaseOuterClass.Release> releasesList = releasesResponse.getReleasesList();
            for (ReleaseOuterClass.Release release : releasesList) {
                String releaseNamespace = release.getNamespace();
                if (!helmContext.getTargetNamespace().equals(releaseNamespace)) {
                    throw new IOException(String.format("Release name has been used in"
                            + " namespace %s", releaseNamespace));
                }
            }
            return true;
        }
        return false;
    }

    private String setValues2Yaml(String setValues) {
        String convert = Props2YAML.fromContent(setValues.replace(",", System.lineSeparator())).convert();
        Yaml yaml = new Yaml();
        Object load = yaml.load(convert);
        return yaml.dump(load);
    }

    private void installHelmRelease(ReleaseManager releaseManager, HelmContext helmContext,
                                    ChartOuterClass.Chart.Builder chart) throws IOException {
        InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
        requestBuilder.setNamespace(helmContext.getTargetNamespace());
        requestBuilder.setTimeout(helmContext.getTimeout());
        requestBuilder.setName(helmContext.getReleaseName());
        requestBuilder.setWait(helmContext.isWait());
        String setValues = helmContext.getSetValues();
        if (StringUtils.isNotBlank(setValues)) {
            String rawValues = setValues2Yaml(setValues);
            requestBuilder.getValuesBuilder().setRaw(rawValues);
        }

        try {
            Future<InstallReleaseResponse> install =
                    releaseManager.install(requestBuilder, chart);
            install.get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    private void updateHelmRelease(ReleaseManager releaseManager, HelmContext helmContext,
                                   ChartOuterClass.Chart.Builder chart) throws IOException {
        UpdateReleaseRequest.Builder builder = UpdateReleaseRequest.newBuilder();

        builder.setName(helmContext.getReleaseName());
        builder.setTimeout(helmContext.getTimeout());
        builder.setWait(helmContext.isWait());
        builder.setForce(true);
        builder.setRecreate(true);

        try {
            Future<UpdateReleaseResponse> update =
                    releaseManager.update(builder, chart);
            update.get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public interface IHelmDeploymentData extends IBaseCommandData {
        String getKubeconfigId();

        HelmContext getHelmContext();

        List<HelmRepositoryEndPoint> getHelmRepositoryEndPoints();
    }
}

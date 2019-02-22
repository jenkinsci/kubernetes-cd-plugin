/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.kubernetes.CustomerTiller;
import com.microsoft.jenkins.kubernetes.helm.HelmContext;
import com.microsoft.jenkins.kubernetes.helm.HelmRepositoryEndPoint;
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
import io.codearte.props2yaml.Props2YAML;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;
import org.microbean.helm.chart.DirectoryChartLoader;
import org.microbean.helm.chart.repository.ChartRepository;
import org.microbean.helm.chart.resolver.ChartResolverException;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HelmDeploymentCommand extends HelmCommand
        implements ICommand<HelmDeploymentCommand.IHelmDeploymentData> {

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
             final Tiller tiller = new CustomerTiller(client, tillerNamespace);
             final ReleaseManager releaseManager = new ReleaseManager(tiller)) {

            String helmCommandType = helmContext.getHelmCommandType();
            switch (helmCommandType) {
                case Constants.HELM_COMMAND_TYPE_INSTALL:
                    // helm chart
                    String chartType = helmContext.getHelmChartType();
                    ChartOuterClass.Chart.Builder chart = null;
                    switch (chartType) {
                        case Constants.HELM_CHART_TYPE_URI:
                            String helmChartLocation = helmContext.getChartLocation();
                            helmChartLocation = context.getJobContext().getWorkspace()
                                    .child(helmChartLocation).getRemote();

                            File file = new File(helmChartLocation);
                            if (!file.exists()) {
                                context.logError(String.format("cannot find helm chart at %s", file.getAbsolutePath()));
                                return;
                            }
                            URI uri = file.toURI();
                            try (final DirectoryChartLoader chartLoader = new DirectoryChartLoader()) {
                                Path path = Paths.get(uri);
                                chart = chartLoader.load(path);
                            } catch (IOException e) {
                                // TODO locating charts fails
                                context.logError(e);
                            }
                            context.logStatus(helmChartLocation);
                            break;

                        case Constants.HELM_CHART_TYPE_REPOSITORY:
                            List<HelmRepositoryEndPoint> helmRepositoryEndPoints = context.getHelmRepositoryEndPoints();
                            String chartName = helmContext.getChartName();
                            String chartVersion = helmContext.getChartVersion();
                            if (!CollectionUtils.isEmpty(helmRepositoryEndPoints)) {
                                for (HelmRepositoryEndPoint helmRepositoryEndPoint : helmRepositoryEndPoints) {
                                    ChartRepository chartRepository =
                                            new ChartRepository(helmRepositoryEndPoint.getName(),
                                                    URI.create(helmRepositoryEndPoint.getUrl()));
                                    try {
                                        chart = chartRepository.resolve(chartName, chartVersion);
                                    } catch (ChartResolverException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            break;
                        default:

                    }


                    createOrUpdateHelm(releaseManager, helmContext, chart);

                    context.setCommandState(CommandState.Success);
                    break;
                case Constants.HELM_COMMAND_TYPE_ROLLBACK:
                    break;
                default:
            }
        } catch (IOException e) {
            context.logError(e);
            context.setCommandState(CommandState.HasError);
        }
    }

    private void createOrUpdateHelm(ReleaseManager releaseManager, HelmContext helmContext,
                                    ChartOuterClass.Chart.Builder chart) {
        if (isHelmReleaseExist(releaseManager, helmContext)) {
            updateHelmRelease(releaseManager, helmContext, chart);
        } else {
            installHelmRelease(releaseManager, helmContext, chart);
        }
    }

    private boolean isHelmReleaseExist(ReleaseManager releaseManager, HelmContext helmContext) {
        ListReleasesRequest.Builder builder = ListReleasesRequest.newBuilder();
        builder.setFilter(helmContext.getReleaseName());
        builder.addAllStatusCodes(Arrays.asList(
                StatusOuterClass.Status.Code.DEPLOYED));
        Iterator<ListReleasesResponse> responses = releaseManager.list(builder.build());
        if (responses.hasNext()) {
            ListReleasesResponse releasesResponse = responses.next();
            ReleaseOuterClass.Release release = releasesResponse.getReleases(0);
            String releaseNamespace = release.getNamespace();
            if (!helmContext.getReleaseName().equals(releaseNamespace)) {
                //TODO release name has been used in other namespace
                System.out.println(releaseNamespace);
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
                                    ChartOuterClass.Chart.Builder chart) {
        final InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
        requestBuilder.setNamespace(helmContext.getTargetNamespace());
        requestBuilder.setTimeout(helmContext.getTimeout());
        requestBuilder.setName(helmContext.getReleaseName());
        requestBuilder.setWait(helmContext.isWait()); // Wait for Pods to be ready
        String rawValues = setValues2Yaml(helmContext.getSetValues());
        requestBuilder.getValuesBuilder().setRaw(rawValues);

        try {
            Future<InstallReleaseResponse> install =
                    releaseManager.install(requestBuilder, chart);
            InstallReleaseResponse installReleaseResponse = install.get();
            ReleaseOuterClass.Release release = installReleaseResponse.getRelease();
            assert release != null;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void updateHelmRelease(ReleaseManager releaseManager, HelmContext helmContext,
                                   ChartOuterClass.Chart.Builder chart) {
        UpdateReleaseRequest.Builder builder = UpdateReleaseRequest.newBuilder();

        builder.setName(helmContext.getReleaseName());
        builder.setTimeout(helmContext.getTimeout());
//        builder.setRecreate(true);
//        builder.setForce(true);
        builder.setWait(helmContext.isWait());

        try {
            Future<UpdateReleaseResponse> update =
                    releaseManager.update(builder, chart);
            UpdateReleaseResponse updateReleaseResponse = update.get();
            ReleaseOuterClass.Release release = updateReleaseResponse.getRelease();
            assert release != null;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public interface IHelmDeploymentData extends IBaseCommandData {
        String getKubeconfigId();

        HelmContext getHelmContext();

        List<HelmRepositoryEndPoint> getHelmRepositoryEndPoints();
    }
}

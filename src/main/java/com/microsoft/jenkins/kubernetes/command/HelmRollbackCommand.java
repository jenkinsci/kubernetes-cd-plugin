/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.kubernetes.helm.HelmContext;
import hapi.services.tiller.Tiller.RollbackReleaseRequest;
import hapi.services.tiller.Tiller.RollbackReleaseResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.apache.commons.lang3.StringUtils;
import org.microbean.helm.ReleaseManager;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HelmRollbackCommand extends HelmCommand
        implements ICommand<HelmRollbackCommand.IHelmRollbackData> {

    @Override
    public void execute(IHelmRollbackData context) {
        HelmContext helmContext = context.getHelmContext();
        String tillerNamespace = helmContext.getTillerNamespace();

        String kubeconfigId = context.getKubeconfigId();
        if (StringUtils.isBlank(kubeconfigId)) {
            throw new IllegalArgumentException("Kubeconfig id is empty, please check your configuration");
        }
        String kubeConfig = getKubeConfigContent(kubeconfigId, context.getJobContext().getOwner());

        try (final DefaultKubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(kubeConfig));
             final ReleaseManager releaseManager = getReleaseManager(client, tillerNamespace)) {
            String rollbackName = helmContext.getRollbackName();
            int revisionNumber = helmContext.getRevisionNumber();
            RollbackReleaseRequest.Builder rollbackBuilder = RollbackReleaseRequest.newBuilder();
            rollbackBuilder.setName(rollbackName);
            rollbackBuilder.setVersion(revisionNumber);
            Future<RollbackReleaseResponse> rollback = releaseManager.rollback(rollbackBuilder.build());

            rollback.get();
        } catch (InterruptedException | ExecutionException | IOException e) {
            context.logError("Failed to execute helm rollback", e);
            context.setCommandState(CommandState.HasError);
        }
        context.setCommandState(CommandState.Success);
    }

    public interface IHelmRollbackData extends IBaseCommandData {

        String getKubeconfigId();

        HelmContext getHelmContext();
    }
}

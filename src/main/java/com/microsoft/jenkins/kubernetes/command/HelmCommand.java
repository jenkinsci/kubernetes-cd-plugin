/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.microsoft.jenkins.kubernetes.credentials.KubeconfigCredentials;
import hudson.model.Item;
import hudson.security.ACL;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

public abstract class HelmCommand {
    protected String getKubeConfigContent(String configId, Item owner) {
        if (StringUtils.isNotBlank(configId)) {
            final KubeconfigCredentials credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            KubeconfigCredentials.class,
                            owner,
                            ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList()),
                    CredentialsMatchers.withId(configId));
            if (credentials == null) {
                throw new IllegalArgumentException("Cannot find kubeconfig credentials with id " + configId);
            }
            return credentials.getContent();
        }
        return null;
    }
}

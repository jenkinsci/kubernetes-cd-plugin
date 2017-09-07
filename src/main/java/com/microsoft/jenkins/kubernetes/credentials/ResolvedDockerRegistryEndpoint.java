/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;

import java.io.Serializable;
import java.net.URL;

/**
 * Docker registry credentials that is fully resolved and serializable.
 * <p>
 * The {@link org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint} is not serializable, and
 * it needs the master build context to resolve the credentials details. This class represents the resolved state
 * of an endpoint, which can be passed to the remote node to build the docker credentials.
 */
public class ResolvedDockerRegistryEndpoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private final URL url;
    private final DockerRegistryToken token;

    public ResolvedDockerRegistryEndpoint(URL url, DockerRegistryToken token) {
        this.url = url;
        this.token = token;
    }

    public URL getUrl() {
        return url;
    }

    public DockerRegistryToken getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "ResolvedDockerRegistryEndpoint{"
                + "url='" + url + '\''
                + ", token=" + token
                + '}';
    }
}

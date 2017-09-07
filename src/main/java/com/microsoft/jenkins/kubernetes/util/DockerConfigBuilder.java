/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import hudson.FilePath;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Builds docker configuration for use of private repository authentication.
 */
public class DockerConfigBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<ResolvedDockerRegistryEndpoint> endpoints;

    public DockerConfigBuilder(List<ResolvedDockerRegistryEndpoint> credentials) {
        this.endpoints = credentials;
    }

    public FilePath buildArchive(FilePath workspace) throws IOException, InterruptedException {
        final FilePath outputFile = workspace.createTempFile("docker", ".tar.gz");

        try (TarArchiveOutputStream out = new TarArchiveOutputStream(new GZIPOutputStream(outputFile.write()))) {
            ObjectNode auths = buildAuthsObject();

            JsonNode config = MAPPER.createObjectNode().set("auths", auths);

            byte[] bytes = config.toString().getBytes(Constants.DEFAULT_CHARSET);

            TarArchiveEntry entry = new TarArchiveEntry(".docker/config.json");
            entry.setSize(bytes.length);
            out.putArchiveEntry(entry);
            out.write(bytes);
            out.closeArchiveEntry();
        }

        return outputFile;
    }

    public String buildDockercfgBase64() throws IOException {
        return Base64.encodeBase64String(buildDockercfgString().getBytes(Constants.DEFAULT_CHARSET));
    }

    public String buildDockercfgString()
            throws IOException {
        ObjectNode auths = buildAuthsObject();
        return auths.toString();
    }

    public ObjectNode buildAuthsObject() throws IOException {
        ObjectNode auths = MAPPER.createObjectNode();
        for (ResolvedDockerRegistryEndpoint endpoint : this.endpoints) {
            DockerRegistryToken token = endpoint.getToken();
            ObjectNode entry = MAPPER.createObjectNode()
                    .put("email", token.getEmail())
                    .put("auth", token.getToken());
            auths.set(endpoint.getUrl().toString(), entry);
        }
        return auths;
    }
}

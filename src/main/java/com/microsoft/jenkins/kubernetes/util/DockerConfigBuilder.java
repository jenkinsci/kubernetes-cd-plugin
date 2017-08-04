/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.util;

import hudson.FilePath;
import hudson.model.Item;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Builds docker configuration for use of private repository authentication.
 */
public class DockerConfigBuilder {
    private final List<DockerRegistryEndpoint> endpoints;

    public DockerConfigBuilder(List<DockerRegistryEndpoint> credentials) {
        this.endpoints = credentials;
    }

    public FilePath buildArchive(FilePath workspace, Item context) throws IOException, InterruptedException {
        final FilePath outputFile = workspace.createTempFile("docker", ".tar.gz");

        try (TarArchiveOutputStream out = new TarArchiveOutputStream(new GZIPOutputStream(outputFile.write()))) {
            JSONObject auths = buildAuthsObject(context);

            JSONObject config = new JSONObject();
            config.put("auths", auths);

            byte[] bytes = config.toString().getBytes(Constants.DEFAULT_CHARSET);

            TarArchiveEntry entry = new TarArchiveEntry(".docker/config.json");
            entry.setSize(bytes.length);
            out.putArchiveEntry(entry);
            out.write(bytes);
            out.closeArchiveEntry();
        }

        return outputFile;
    }

    public String buildDockercfgBase64(Item context) throws IOException {
        return Base64.encodeBase64String(buildDockercfgString(context).getBytes(Constants.DEFAULT_CHARSET));
    }

    public String buildDockercfgString(Item context)
            throws IOException {
        JSONObject auths = buildAuthsObject(context);
        return auths.toString();
    }

    public JSONObject buildAuthsObject(Item context) throws IOException {
        JSONObject auths = new JSONObject();
        for (DockerRegistryEndpoint endpoint : this.endpoints) {
            DockerRegistryToken token = endpoint.getToken(context);

            if (token == null) {
                // no credentials filled for this entry
                continue;
            }

            JSONObject entry = new JSONObject()
                    .element("email", token.getEmail())
                    .element("auth", token.getToken());
            auths.put(endpoint.getEffectiveUrl().toString(), entry);
        }
        return auths;
    }
}

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
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DockerConfigBuilder}.
 */
public class DockerConfigBuilderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResolvedDockerRegistryEndpoint[] endpoints;

    @Before
    public void setup() throws Exception {
        endpoints = new ResolvedDockerRegistryEndpoint[]{
                new ResolvedDockerRegistryEndpoint(
                        new URL("https://index.docker.io/v1/"),
                        new DockerRegistryToken("user", "dXNlcjpwYXNzd29yZA==")),
                new ResolvedDockerRegistryEndpoint(
                        new URL("http://acr.azurecr.io"),
                        new DockerRegistryToken("anotherUser", "YW5vdGhlclVzZXI6aGFoYWhh"))
        };
    }

    @Test
    public void testBuildArchive() throws Exception {
        FilePath workspace = new FilePath(new File(System.getProperty("java.io.tmpdir")));

        FilePath empty = new DockerConfigBuilder(Collections.<ResolvedDockerRegistryEndpoint>emptyList()).buildArchive(workspace);
        verifyArchive(empty);

        FilePath archive = new DockerConfigBuilder(Arrays.asList(endpoints)).buildArchive(workspace);
        verifyArchive(archive, endpoints);
    }

    private void verifyArchive(FilePath archiveFile, ResolvedDockerRegistryEndpoint... endpoints) throws Exception {
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GZIPInputStream(archiveFile.read()))) {
            TarArchiveEntry entry = tarInput.getNextTarEntry();
            assertEquals(".docker/config.json", entry.getName());
            JsonNode jsonNode = MAPPER.readTree(tarInput);
            assertTrue(jsonNode instanceof ObjectNode);

            assertEquals(1, jsonNode.size());
            ObjectNode auths = ObjectNode.class.cast(jsonNode.get("auths"));
            verifyAuthsObject(auths, endpoints);
        } finally {
            archiveFile.delete();
        }
    }

    private void verifyAuthsObject(JsonNode node, ResolvedDockerRegistryEndpoint... endpoints) {
        assertEquals(endpoints.length, node.size());
        for (ResolvedDockerRegistryEndpoint endpoint : endpoints) {
            ObjectNode auth = ObjectNode.class.cast(node.get(endpoint.getUrl().toString()));
            assertNotNull(auth);
            assertEquals(endpoint.getToken().getEmail(), auth.get("email").asText());
            assertEquals(endpoint.getToken().getToken(), auth.get("auth").asText());
        }
    }

    @Test
    public void testBuildAuthObject() throws Exception {
        ObjectNode node = new DockerConfigBuilder(Collections.<ResolvedDockerRegistryEndpoint>emptyList()).buildAuthsObject();
        verifyAuthsObject(node);

        node = new DockerConfigBuilder(Arrays.asList(endpoints)).buildAuthsObject();
        verifyAuthsObject(node, endpoints);
    }

    @Test
    public void testBuildDockercfgBase64() throws Exception {
        String base64 = new DockerConfigBuilder(Arrays.asList(endpoints)).buildDockercfgBase64();
        JsonNode node = MAPPER.readTree(Base64.decodeBase64(base64));
        verifyAuthsObject(node, endpoints);
    }
}

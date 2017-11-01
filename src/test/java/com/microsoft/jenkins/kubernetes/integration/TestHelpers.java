/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.integration;

import hudson.FilePath;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class TestHelpers {
    public static String loadProperty(final String name) {
        return loadProperty(name, "");
    }

    public static String loadProperty(final String name, final String defaultValue) {
        final String value = System.getProperty(name);
        if (StringUtils.isEmpty(value)) {
            return loadEnv(name, defaultValue);
        }
        return value;
    }

    public static String loadEnv(final String name, final String defaultValue) {
        String value = System.getenv(name);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return value;
    }

    public static String generateRandomString(int length) {
        String uuid = UUID.randomUUID().toString();
        return uuid.replaceAll("[^a-z0-9]", "a").substring(0, length);
    }

    public static void loadFile(Class<?> clazz, FilePath workspace, String path) throws Exception {
        loadFile(clazz, workspace, path, path);
    }

    public static void loadFile(Class<?> clazz, FilePath workspace, String sourcePath, String destinationPath) throws Exception {
        FilePath dest = new FilePath(workspace, destinationPath);
        dest.getParent().mkdirs();
        try (InputStream in = clazz.getResourceAsStream(sourcePath);
             OutputStream out = dest.write()) {
            IOUtils.copy(in, out);
        }
    }

    private TestHelpers() {
        // hide constructor
    }
}

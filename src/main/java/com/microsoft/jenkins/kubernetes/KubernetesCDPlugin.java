/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsClientFactory;
import hudson.Plugin;

import java.util.HashMap;
import java.util.Map;

public class KubernetesCDPlugin extends Plugin {
    public static void sendEvent(String item, String action, String... properties) {
        Map<String, String> props = new HashMap<>();
        for (int i = 1; i < properties.length; ++i) {
            props.put(properties[i - 1], properties[i]);
        }
        sendEvent(item, action, props);
    }

    public static void sendEvent(String item, String action, Map<String, String> properties) {
        AppInsightsClientFactory.getInstance(KubernetesCDPlugin.class)
                .sendEvent(item, action, properties, false);
    }
}

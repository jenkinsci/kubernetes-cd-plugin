package com.microsoft.jenkins.kubernetes;

import java.util.HashMap;
import java.util.Map;

public enum DeployType {
    UNKNOWN("unknown"),
    KUBERNETES("kubernetes"),
    HELM("helm");

    private String name;

    DeployType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, DeployType> LOOKUP = new HashMap<>();

    static {
        for (DeployType ref : DeployType.values()) {
            LOOKUP.put(ref.getName(), ref);
        }
    }

    public static DeployType get(String name) {
        DeployType result;
        try {
            result = LOOKUP.get(name);
        } catch (Exception e) {
            return DeployType.UNKNOWN;
        }
        return result;
    }
}

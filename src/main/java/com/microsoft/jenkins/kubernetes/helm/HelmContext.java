/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.helm;

import java.util.List;

public final class HelmContext {
    private String chartLocation;
    private String chartName;
    private String chartVersion;
    private String targetNamespace;
    private String tillerNamespace;
    private String releaseName;
    private boolean wait;
    private long timeout;
    private List<HelmRepositoryEndPoint> helmRepositoryEndPoints;
    private String helmCommandType;
    private String helmChartType;
    private int revisionNumber;
    private String rollbackName;

    private HelmContext(String chartLocation,
                        String chartName,
                        String chartVersion,
                        String targetNamespace,
                        String tillerNamespace,
                        String releaseName,
                        boolean wait,
                        long timeout,
                        List<HelmRepositoryEndPoint> helmRepositoryEndPoints,
                        String helmCommandType,
                        String helmChartType,
                        int revisionNumber,
                        String rollbackName) {
        this.chartLocation = chartLocation;
        this.chartName = chartName;
        this.chartVersion = chartVersion;
        this.targetNamespace = targetNamespace;
        this.tillerNamespace = tillerNamespace;
        this.releaseName = releaseName;
        this.wait = wait;
        this.timeout = timeout;
        this.helmRepositoryEndPoints = helmRepositoryEndPoints;
        this.helmCommandType = helmCommandType;
        this.helmChartType = helmChartType;
        this.revisionNumber = revisionNumber;
        this.rollbackName = rollbackName;
    }

    public static class Builder {
        private String nestChartLocation;
        private String nestChartName;
        private String nestChartVersion;
        private String nestTargetNamespace;
        private String nestTillerNamespace;
        private String nestReleaseName;
        private boolean nestWait;
        private long nestTimeout;
        private List<HelmRepositoryEndPoint> nestHelmRepositoryEndPoints;
        private String nestHelmCommandType;
        private String nestHelmChartType;
        private int nestRevisionNumber;
        private String nestRollbackName;

        public Builder(String nestChartLocation) {
            this.nestChartLocation = nestChartLocation;
        }

        public Builder withChartName(String chartName) {
            this.nestChartName = chartName;
            return this;
        }

        public Builder withChartVersion(String chartVersion) {
            this.nestChartVersion = chartVersion;
            return this;
        }

        public Builder withTargetNamespace(String targetNamespace) {
            this.nestTargetNamespace = targetNamespace;
            return this;
        }

        public Builder withTillerNamespace(String tillerNamespace) {
            this.nestTillerNamespace = tillerNamespace;
            return this;
        }

        public Builder withReleaseName(String releaseName) {
            this.nestReleaseName = releaseName;
            return this;
        }

        public Builder withWait(boolean wait) {
            this.nestWait = wait;
            return this;
        }

        public Builder withTimeout(long timeout) {
            this.nestTimeout = timeout;
            return this;
        }

        public Builder withHelmRepositoryEndpoints(List<HelmRepositoryEndPoint> helmRepositoryEndpoints) {
            this.nestHelmRepositoryEndPoints = helmRepositoryEndpoints;
            return this;
        }

        public Builder withHelmCommandType(String helmCommandType) {
            this.nestHelmCommandType = helmCommandType;
            return this;
        }

        public Builder withHelmChartType(String helmChartType) {
            this.nestHelmChartType = helmChartType;
            return this;
        }

        public Builder withRevisionNumber(int revisionNumber) {
            this.nestRevisionNumber = revisionNumber;
            return this;
        }

        public Builder withRollbackName(String rollbackName) {
            this.nestRollbackName = rollbackName;
            return this;
        }

        public HelmContext build() {
            return new HelmContext(nestChartLocation, nestChartName, nestChartVersion,
                    nestTargetNamespace, nestTillerNamespace, nestReleaseName, nestWait,
                    nestTimeout, nestHelmRepositoryEndPoints, nestHelmCommandType,
                    nestHelmChartType, nestRevisionNumber, nestRollbackName);
        }
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getChartLocation() {
        return chartLocation;
    }

    public String getChartName() {
        return chartName;
    }

    public String getChartVersion() {
        return chartVersion;
    }

    public String getTillerNamespace() {
        return tillerNamespace;
    }

    public boolean isWait() {
        return wait;
    }

    public List<HelmRepositoryEndPoint> getHelmRepositoryEndPoints() {
        return helmRepositoryEndPoints;
    }

    public String getHelmCommandType() {
        return helmCommandType;
    }

    public String getHelmChartType() {
        return helmChartType;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public String getRollbackName() {
        return rollbackName;
    }

    @Override
    public String toString() {
        return "HelmContext{"
                + "chartLocation='" + chartLocation + '\''
                + ", targetNamespace='" + targetNamespace + '\''
                + ", tillerNamespace='" + tillerNamespace + '\''
                + ", releaseName='" + releaseName + '\''
                + ", wait=" + wait
                + ", timeout=" + timeout
                + '}';
    }
}

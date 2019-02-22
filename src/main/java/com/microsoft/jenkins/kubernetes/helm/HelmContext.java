/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.helm;

import com.microsoft.jenkins.kubernetes.util.Constants;

import java.util.List;

public final class HelmContext {
    /**
     * A path to a packaged chart, a path to an unpacked chart directory or a URL.
     */
    private String chartLocation;
    /**
     * Name of a helm chart, used in chart reference.
     */
    private String chartName;
    /**
     * Version of a helm chart, used in chart reference.
     */
    private String chartVersion;
    /**
     * Namespace which the helm chart will be installed into.
     */
    private String targetNamespace;
    /**
     * Namespace where the tiller is installed.
     */
    private String tillerNamespace;
    /**
     * Release name of a helm chart installation.
     */
    private String releaseName;
    /**
     * If this is true, will wait until all Pods, PVCs, and Services are in a ready state
     * before marking the release as successful. It will wait for as long as timeout.
     */
    private boolean wait;
    /**
     * Time in seconds to wait for any individual Kubernetes operation (default 300).
     */
    private long timeout;
    /**
     * Set values for the helm chart in yaml format.
     */
    private String setValues;
    /**
     * List of helm repositories configured in this plugin.
     */
    private List<HelmRepositoryEndPoint> helmRepositoryEndPoints;
    /**
     * Type of helm command.
     */
    private String helmCommandType;
    /**
     * Way to locate the target helm chart.
     */
    private String helmChartType;
    /**
     * The version which the release wants to be rollback to.
     */
    private int revisionNumber;
    /**
     * The name of the release needed to be rollback.
     */
    private String rollbackName;

    private HelmContext(String chartLocation,
                        String chartName,
                        String chartVersion,
                        String targetNamespace,
                        String tillerNamespace,
                        String releaseName,
                        boolean wait,
                        long timeout,
                        String setValues,
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
        this.setValues = setValues;
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
        private long nestTimeout = Constants.DEFAULT_HELM_TIMEOUT;
        private String nestSetValues;
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

        public Builder withSetValues(String setValues) {
            this.nestSetValues = setValues;
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
                    nestTimeout, nestSetValues, nestHelmRepositoryEndPoints, nestHelmCommandType,
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

    public String getSetValues() {
        return setValues;
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
                + ", chartName='" + chartName + '\''
                + ", chartVersion='" + chartVersion + '\''
                + ", targetNamespace='" + targetNamespace + '\''
                + ", tillerNamespace='" + tillerNamespace + '\''
                + ", releaseName='" + releaseName + '\''
                + ", wait=" + wait
                + ", timeout=" + timeout
                + ", setValues=" + setValues
                + ", helmRepositoryEndPoints=" + helmRepositoryEndPoints
                + ", helmCommandType='" + helmCommandType + '\''
                + ", helmChartType='" + helmChartType + '\''
                + ", revisionNumber=" + revisionNumber
                + ", rollbackName='" + rollbackName + '\''
                + '}';
    }
}

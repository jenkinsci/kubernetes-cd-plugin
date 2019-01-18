/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes;

import java.util.List;

public final class HelmContext {
    private String chartLocation;
    private String targetNamespace;
    private String tillerNamespace;
    private String releaseName;
    private boolean wait;
    private long timeout;
    private List<HelmRepositoryEndPoint> helmRepositoryEndPoints;

    private HelmContext(String chartLocation,
                        String targetNamespace,
                        String tillerNamespace,
                        String releaseName,
                        boolean wait,
                        long timeout,
                        List<HelmRepositoryEndPoint> helmRepositoryEndPoints) {
        this.chartLocation = chartLocation;
        this.targetNamespace = targetNamespace;
        this.tillerNamespace = tillerNamespace;
        this.releaseName = releaseName;
        this.wait = wait;
        this.timeout = timeout;
        this.helmRepositoryEndPoints = helmRepositoryEndPoints;
    }

    public static class Builder {
        private String nestChartLocation;
        private String nestTargetNamespace;
        private String nestTillerNamespace;
        private String nestReleaseName;
        private boolean nestWait;
        private long nestTimeout;
        private List<HelmRepositoryEndPoint> nestHelmRepositoryEndPoints;

        public Builder(String nestChartLocation) {
            this.nestChartLocation = nestChartLocation;
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

        public HelmContext build() {
            return new HelmContext(nestChartLocation, nestTargetNamespace,
                    nestTillerNamespace, nestReleaseName, nestWait, nestTimeout,
                    nestHelmRepositoryEndPoints);
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

    public String getTillerNamespace() {
        return tillerNamespace;
    }

    public boolean isWait() {
        return wait;
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

package com.microsoft.jenkins.kubernetes;

import hapi.chart.ChartOuterClass;

public class HelmContext {
    private String namespace;
    private String releaseName;
    private ChartOuterClass.Chart.Builder chart;
    private String rawValue;
    private long timeout;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public ChartOuterClass.Chart.Builder getChart() {
        return chart;
    }

    public void setChart(ChartOuterClass.Chart.Builder chart) {
        this.chart = chart;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "HelmContext{"
                + "namespace='" + namespace + '\''
                + ", releaseName='" + releaseName + '\''
                + ", chart=" + chart
                + ", rawValue='" + rawValue + '\''
                + ", timeout=" + timeout
                + '}';
    }
}

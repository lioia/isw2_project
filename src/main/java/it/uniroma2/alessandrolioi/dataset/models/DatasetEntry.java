package it.uniroma2.alessandrolioi.dataset.models;

import it.uniroma2.alessandrolioi.common.Metric;

import java.util.HashMap;
import java.util.Map;

public final class DatasetEntry {
    private final Map<Metric, String> metrics;
    private boolean buggy;

    public DatasetEntry() {
        this.metrics = new HashMap<>();
        this.buggy = false;
    }

    public Map<Metric, String> metrics() {
        return metrics;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public void setBuggy(boolean buggy) {
        this.buggy = buggy;
    }
}

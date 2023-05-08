package it.uniroma2.alessandrolioi.dataset.models;

import java.util.HashMap;
import java.util.Map;

public final class DatasetEntry {
    private final Map<String, String> metrics;
    private boolean buggy;

    public DatasetEntry() {
        this.metrics = new HashMap<>();
        this.buggy = false;
    }

    public Map<String, String> metrics() {
        return metrics;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public void setBuggy(boolean buggy) {
        this.buggy = buggy;
    }
}

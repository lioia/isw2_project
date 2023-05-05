package it.uniroma2.alessandrolioi.dataset.models;

import java.util.HashMap;
import java.util.Map;

public final class DatasetEntry {
    private final Map<String, String> metrics;
    private final boolean buggy;

    public DatasetEntry() {
        this.metrics = new HashMap<>();
        this.buggy = false;
    }

    public Map<String, String> metrics() {
        return metrics;
    }

    public boolean buggy() {
        return buggy;
    }
}

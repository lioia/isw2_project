package it.uniroma2.alessandrolioi.dataset.models;

import java.util.Map;

public record DatasetEntry(
        // Map<Metric, MetricValue>
        Map<String, String> metrics,
        boolean buggy) {
}

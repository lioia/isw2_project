package it.uniroma2.alessandrolioi.dataset.models;

import it.uniroma2.alessandrolioi.common.Metric;

public record MetricValue(String aClass, int version, Metric metric, Object value) {
}

package it.uniroma2.alessandrolioi.analysis.models;

import it.uniroma2.alessandrolioi.common.Metric;

import java.util.Map;

public record CsvEntry(String version, String name, Map<Metric, String> fields, boolean buggy) {
}

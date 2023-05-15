package it.uniroma2.alessandrolioi.analysis.models;

import java.util.Map;

public record CsvEntry(String version, String name, Map<String, String> fields, boolean buggy) {
}

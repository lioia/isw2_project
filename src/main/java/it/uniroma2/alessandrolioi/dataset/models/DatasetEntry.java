package it.uniroma2.alessandrolioi.dataset.models;

import java.util.Map;

public record DatasetEntry(
        String name,
        // Map<Field, FieldValue>
        Map<String, String> fields,
        boolean buggy) {
}

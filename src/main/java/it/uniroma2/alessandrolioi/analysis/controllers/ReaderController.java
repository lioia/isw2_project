package it.uniroma2.alessandrolioi.analysis.controllers;

import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;
import it.uniroma2.alessandrolioi.common.DatasetPaths;
import it.uniroma2.alessandrolioi.common.Metric;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ReaderController {
    public Map<Integer, List<CsvEntry>> loadCsv(String project) throws CsvException {
        try {
            Map<Integer, List<CsvEntry>> entries = new HashMap<>();
            Path path = DatasetPaths.fromProject(project).resolve("dataset.csv");
            List<String> lines = Files.readAllLines(path);
            int version = 1;
            List<CsvEntry> versionEntries = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split(",");
                // Read the version from the current line
                int newVersion = Integer.parseInt(values[0]);
                // There is another version
                if (version != newVersion) {
                    // Save entries for current version
                    entries.put(version, versionEntries);
                    // Create new list for the entries of the new version
                    versionEntries = new ArrayList<>();
                    // Update version value
                    version = newVersion;
                }
                // Read current entry
                versionEntries.add(readEntry(lines.get(i)));
            }
            // Add the last version to the map
            entries.put(version, versionEntries);
            return entries;
        } catch (IOException e) {
            throw new CsvException("Could not read file", e);
        }
    }

    private CsvEntry readEntry(String line) {
        String[] values = line.split(",");
        String version = values[0];
        String fileName = values[1];
        boolean buggy = Objects.equals(values[values.length - 1].toLowerCase(), "true");
        Map<Metric, String> fields = new EnumMap<>(Metric.class);
        // Skipping for two values (Version, File Name) and last value (Buggy)
        for (int j = 2; j < values.length - 1; j++)
            fields.put(Metric.values()[j - 2], values[j]);
        return new CsvEntry(version, fileName, fields, buggy);
    }
}

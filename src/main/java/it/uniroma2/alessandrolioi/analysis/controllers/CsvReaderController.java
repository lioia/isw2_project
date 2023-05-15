package it.uniroma2.alessandrolioi.analysis.controllers;

import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CsvReaderController {
    public Map<Integer, List<CsvEntry>> readCsv(String file) throws CsvException {
        try {
            Map<Integer, List<CsvEntry>> entries = new HashMap<>();
            Path path = Paths.get(file);
            List<String> lines = Files.readAllLines(path);
            String[] header = lines.get(0).split(",");
            String[] fields = Arrays.copyOfRange(header, 2, header.length - 2);
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
                versionEntries.add(readEntry(lines.get(i), fields));
            }
            // Add the last version to the map
            entries.put(version, versionEntries);
            return entries;
        } catch (IOException e) {
            throw new CsvException("Could not read file", e);
        }
    }

    private CsvEntry readEntry(String line, String[] params) {
        String[] values = line.split(",");
        String version = values[0];
        String fileName = values[1];
        boolean buggy = Objects.equals(values[values.length - 1].toLowerCase(), "true");
        Map<String, String> fields = new HashMap<>();
        // Skipping for two values (Version, File Name) and last value (Buggy)
        for (int j = 2; j < values.length - 2; j++)
            fields.put(params[j - 2], values[j]);
        return new CsvEntry(version, fileName, fields, buggy);
    }
}

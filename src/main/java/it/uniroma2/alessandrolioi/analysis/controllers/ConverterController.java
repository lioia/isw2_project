package it.uniroma2.alessandrolioi.analysis.controllers;

import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;
import it.uniroma2.alessandrolioi.common.DatasetPaths;
import it.uniroma2.alessandrolioi.common.Metric;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConverterController {
    public Map<Integer, List<CsvEntry>> readCsv(String project, int lastRelease) throws CsvException {
        try {
            Map<Integer, List<CsvEntry>> entries = new HashMap<>();
            String file = "dataset/%s/%d.csv".formatted(project, lastRelease);
            Path path = Paths.get(file);
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

    public void writeToArff(String project, Map<Integer, List<CsvEntry>> entries, int lastRelease) throws ArffException {
        List<String> attributes = Arrays.stream(Metric.values()).map(m -> "@attribute %s numeric".formatted(m.name())).toList();
        List<String> testingData = entries.get(lastRelease).stream().map(this::entryFieldsToArff).toList();
        List<String> trainingData = new ArrayList<>();
        for (int i = 1; i < lastRelease; i++)
            trainingData.addAll(entries.get(i).stream().map(this::entryFieldsToArff).toList());
        String testingFile = "testing-%d.arff".formatted(lastRelease);
        String trainingFile = "training-%d.arff".formatted(lastRelease);
        try {
            writeFile(testingFile, project, attributes, testingData);
            writeFile(trainingFile, project, attributes, trainingData);
        } catch (IOException e) {
            throw new ArffException("Could not write arff files", e);
        }
    }

    public Instances loadInstance(String project, int testingRelease, String instanceType) throws ArffException {
        try {
            String filename = "dataset/%s/%s-%d.arff".formatted(project, instanceType, testingRelease);
            Instances instance = ConverterUtils.DataSource.read(filename);
            if (instance.classIndex() == -1)
                instance.setClassIndex(instance.numAttributes() - 1);
            return instance;
        } catch (Exception e) {
            throw new ArffException("Could not load arff file", e);
        }
    }

    private void writeFile(String filename, String project, List<String> attributes, List<String> entries) throws IOException {
        if (!Files.exists(Paths.get("dataset")) || !Files.exists(DatasetPaths.fromProject(project)))
            throw new IOException("dataset folder does not exists");
        Path path = DatasetPaths.fromProject(project).resolve(filename);
        String text = "@relation %s\n".formatted(project) +
                String.join("\n", attributes) + "\n" +
                "@attribute Buggy {true,false}\n" +
                "@data\n" +
                String.join("\n", entries);
        Files.write(path, text.getBytes());
    }

    private String entryFieldsToArff(CsvEntry entry) {
        List<String> orderedValues = new ArrayList<>();
        for (Metric field : Metric.values()) {
            String value = entry.fields().get(field);
            orderedValues.add(value);
        }
        return String.join(",", orderedValues) + ",%s".formatted(entry.buggy());
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

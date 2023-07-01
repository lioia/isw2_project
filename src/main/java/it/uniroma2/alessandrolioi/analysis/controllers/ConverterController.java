package it.uniroma2.alessandrolioi.analysis.controllers;

import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;
import it.uniroma2.alessandrolioi.common.DatasetPaths;
import it.uniroma2.alessandrolioi.common.Metric;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConverterController {
    public void writeToArff(String project, Map<Integer, List<CsvEntry>> oracleEntries,
                            Map<Integer, List<CsvEntry>> entries, int lastRelease) throws ArffException {
        List<String> attributes = Arrays.stream(Metric.values()).map(m -> "@attribute %s numeric".formatted(m.name())).toList();
        List<String> testingData = oracleEntries.get(lastRelease).stream().map(this::entryFieldsToArff).toList();
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

    private void writeFile(String filename, String project, List<String> attributes, List<String> entries) throws IOException {
        if (!Files.exists(Paths.get("dataset")) || !Files.exists(DatasetPaths.fromProject(project)))
            throw new IOException("dataset folder does not exists");
        Path arffFolder = DatasetPaths.fromProject(project).resolve("arff");
        Files.createDirectories(arffFolder);
        Path path = arffFolder.resolve(filename);
        String text = "@relation %s%n".formatted(project) +
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
}

package it.uniroma2.alessandrolioi.dataset.controllers;

import it.uniroma2.alessandrolioi.common.DatasetPaths;
import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.common.Metric;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WriterController {
    public String writeToText(List<Pair<JiraVersion, GitCommitEntry>> revisions,
                              Map<String, List<DatasetEntry>> entries, int numberOfVersions) {
        String header = writeHeader();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < numberOfVersions; i++) {
            GitCommitEntry revision = revisions.get(i).second();
            for (String aClass : revision.classList()) {
                String value = writeEntry(i, aClass, entries.get(aClass).get(i));
                values.add(value);
            }
        }
        return "%s%n%s".formatted(header, String.join("\n", values));
    }

    public void writeToFile(String project, String text, String name) throws DatasetWriterException {
        try {
            Path datasetFolder = DatasetPaths.fromProject(project).resolve("datasets");
            Files.createDirectories(datasetFolder);
            Path output = datasetFolder.resolve("%s.csv".formatted(name));
            Files.write(output, text.getBytes());
        } catch (IOException e) {
            throw new DatasetWriterException("Could not write file", e);
        }
    }

    private String writeHeader() {
        List<String> metrics = Arrays.stream(Metric.values()).map(Metric::name).toList();
        return "Version,File_Name,%s,Buggy".formatted(String.join(",", metrics));
    }

    private String writeEntry(int version, String className, DatasetEntry entry) {
        List<String> metrics = new ArrayList<>();
        for (Metric value : Metric.values())
            metrics.add(entry.metrics().get(value));
        return "%d,%s,%s,%s".formatted(version + 1, className, String.join(",", metrics), entry.isBuggy());
    }
}

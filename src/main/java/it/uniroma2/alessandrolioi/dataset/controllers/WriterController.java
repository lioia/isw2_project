package it.uniroma2.alessandrolioi.dataset.controllers;

import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.common.Metric;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WriterController {
    public void writeToFile(String project, List<Pair<JiraVersion, GitCommitEntry>> revisions,
                            Map<String, List<DatasetEntry>> entries, int numberOfVersions) throws DatasetWriterException {
        try {
            Files.createDirectories(Paths.get("dataset", project));

            String header = writeHeader();
            List<String> values = new ArrayList<>();
            for (int i = 0; i < numberOfVersions; i++) {
                GitCommitEntry revision = revisions.get(i).second();
                for (String aClass : revision.classList()) {
                    String value = writeEntry(i, aClass, entries.get(aClass).get(i));
                    values.add(value);
                }
            }
            String text = "%s\n%s".formatted(header, String.join("\n", values));

            Path output = Paths.get("dataset", project, "%d.csv".formatted(numberOfVersions));
            Files.write(output, text.getBytes());
        } catch (IOException e) {
            throw new DatasetWriterException("", e);
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
        return "%d,%s,%s,%s".formatted(version, className, String.join(",", metrics), entry.isBuggy());
    }
}

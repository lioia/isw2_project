package it.uniroma2.alessandrolioi.dataset.controllers;

import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class WriterController {
    public void writeToFile(String output, List<GitCommitEntry> revisions, List<String> metrics, Map<String, List<DatasetEntry>> entries, int numberOfVersions) throws DatasetWriterException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writeHeader(writer, metrics);
            // For every release (sorted)
            for (int i = 0; i < numberOfVersions; i++) {
                // Get current release/revision
                GitCommitEntry revision = revisions.get(i);
                for (String aClass : revision.classList()) {
                    DatasetEntry entry = entries.get(aClass).get(i);
                    // Write number of release and class name
                    writer.write("%d,%s".formatted(i + 1, aClass));
                    // Write Metrics and Buggy
                    writeMetrics(writer, metrics, entry);
                }
            }
        } catch (IOException e) {
            throw new DatasetWriterException("Could not create or load file %s".formatted(output), e);
        }
    }

    private void writeHeader(BufferedWriter writer, List<String> metrics) throws DatasetWriterException {
        try {
            writer.write("Version, File Name");
            for (String metric : metrics) writer.write(",%s".formatted(metric));
            writer.write(",Buggy\n");
        } catch (IOException e) {
            throw new DatasetWriterException("Could not write header to file", e);
        }
    }

    private void writeMetrics(BufferedWriter writer, List<String> metrics, DatasetEntry entry) throws DatasetWriterException {
        try {
            // For every metric
            for (String metric : metrics) {
                String value = entry.metrics().get(metric);
                // Write metric
                writer.write(",%s".formatted(value));
            }
            // Write buggy field
            writer.write(",%s\n".formatted(entry.isBuggy()));
        } catch (IOException e) {
            throw new DatasetWriterException("Could not write entry to file", e);
        }
    }
}

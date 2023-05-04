package it.uniroma2.alessandrolioi.dataset;

import it.uniroma2.alessandrolioi.dataset.controllers.WriterController;
import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;

public class DatasetBuilder {
    private final Map<JiraVersion, GitCommitEntry> revisions;
    private final Git git;

    // Map<ClassName, DatasetEntry>
    private final Map<JiraVersion, List<DatasetEntry>> entries;
    private final List<String> metrics;

    public DatasetBuilder(Map<JiraVersion, GitCommitEntry> revisions, Git git) {
        this.revisions = revisions;
        this.git = git;
        this.entries = new HashMap<>();
        metrics = new ArrayList<>();
    }

    // X: type of metric
    public void applyLOCMetric() {
        /*
         * Pseudocode:
         *   loop for every pair of consecutive releases:
         *       loop for every class
         *           calculate metric
         *           add metric to entries[class].fields
         *    add field to this.metrics
         * */
    }

    public void writeToFile(String output) throws DatasetWriterException {
        // Get the versions
        List<JiraVersion> versions = new ArrayList<>(revisions.keySet().stream().toList());
        // Sort versions for release date
        versions.sort(Comparator.comparing(JiraVersion::releaseDate));

        WriterController controller = new WriterController();
        controller.writeToFile(output, versions, metrics, entries);
    }
}

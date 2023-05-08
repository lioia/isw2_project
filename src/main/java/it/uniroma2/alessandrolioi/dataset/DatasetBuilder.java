package it.uniroma2.alessandrolioi.dataset;

import it.uniroma2.alessandrolioi.dataset.controllers.MetricController;
import it.uniroma2.alessandrolioi.dataset.controllers.WriterController;
import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.exceptions.MetricException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;

public class DatasetBuilder {
    // Sorted list of revisions corresponding to a Jira release
    private final List<GitCommitEntry> revisions;
    private final Git git;

    // Maps class name to its metrics. i-th element in the list is the entry for the i-th version
    private final Map<String, List<DatasetEntry>> entries;
    private final List<String> metrics;

    public DatasetBuilder(Map<JiraVersion, GitCommitEntry> revisionsMap, Git git) {
        this.revisions = new ArrayList<>();
        this.git = git;
        this.metrics = new ArrayList<>();
        this.entries = new HashMap<>();

        List<JiraVersion> sortedVersions = new ArrayList<>(revisionsMap.keySet().stream().toList());
        sortedVersions.sort(Comparator.comparing(JiraVersion::releaseDate)); // Sort versions for release date

        // Initialize `revisions`
        for (JiraVersion version : sortedVersions) {
            GitCommitEntry revision = revisionsMap.get(version);

            // Initialize `entries` map
            for (String aClass : revision.classList()) {
                List<DatasetEntry> datasetEntries = new ArrayList<>();
                for (int i = 0; i < sortedVersions.size(); i++)
                    datasetEntries.add(i, new DatasetEntry());
                entries.put(aClass, datasetEntries);
            }

            this.revisions.add(revision);
        }
    }

    /*
     * Pseudocode:
     *   loop for every pair of consecutive releases:
     *       loop for every class
     *           calculate metric
     *           add metric to entries[class].fields
     *    add field to this.metrics
     * */
    public void applyLOCMetric() throws MetricException {
        String metric = "LOC";
        MetricController controller = new MetricController();
        controller.applyLOCMetric(metric, git, revisions, entries);
        metrics.add(metric);
    }

    public void applyLOCTouchedMetric() throws MetricException {
        String metric = "LOC Touched";
        MetricController controller = new MetricController();
        controller.applyDifferenceMetric(metric, git, revisions, entries, diff -> {
            int locTouched = 0;
            if (diff != null)
                locTouched = diff.added() + diff.deleted();
            return Integer.toString(locTouched);
        });
        metrics.add(metric);
    }

    public void applyChurnMetric() throws MetricException {
        String metric = "Churn";
        MetricController controller = new MetricController();
        controller.applyDifferenceMetric(metric, git, revisions, entries, diff -> {
            int churn = 0;
            if (diff != null)
                churn = diff.added() - diff.deleted();
            return Integer.toString(churn);
        });
        metrics.add(metric);
    }

    public void applyMaxLOCAddedMetric() throws MetricException {
        String metric = "Max LOC Added";
        MetricController controller = new MetricController();
        controller.applyCumulativeMetric(metric, git, revisions, entries, diffs -> {
            Optional<Integer> max = diffs.stream().map(diff -> diff.added() + diff.deleted()).max(Comparator.naturalOrder());
            return max.map(Object::toString).orElse("0");
        });
        metrics.add(metric);
    }

    public void applyMaxChurnMetric() throws MetricException {
        String metric = "Max Churn Added";
        MetricController controller = new MetricController();
        controller.applyCumulativeMetric(metric, git, revisions, entries, diffs -> {
            Optional<Integer> max = diffs.stream().map(diff -> diff.added() - diff.deleted()).max(Comparator.naturalOrder());
            return max.map(Object::toString).orElse("0");
        });
        metrics.add(metric);
    }

    public void writeToFile(String output) throws DatasetWriterException {
        WriterController controller = new WriterController();
        controller.writeToFile(output, revisions, metrics, entries);
    }
}

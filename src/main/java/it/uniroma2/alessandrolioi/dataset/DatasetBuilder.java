package it.uniroma2.alessandrolioi.dataset;

import it.uniroma2.alessandrolioi.dataset.controllers.MetricController;
import it.uniroma2.alessandrolioi.dataset.controllers.WriterController;
import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.exceptions.MetricException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.exceptions.GitFileException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;

public class DatasetBuilder {
    private final List<JiraVersion> sortedVersions;
    private final Map<JiraVersion, GitCommitEntry> revisions;
    private final Git git;

    // Maps Jira version to the list of classes in the corresponding revision
    private final Map<Integer, List<String>> entryKeys;
    // Maps class name to its metrics. i-th element in the list is the entry for the i-th version
    private final Map<String, List<DatasetEntry>> entryValues;
    private final List<String> metrics;

    public DatasetBuilder(Map<JiraVersion, GitCommitEntry> revisions, Git git) throws GitLogException {
        this.revisions = revisions;
        this.git = git;
        this.sortedVersions = new ArrayList<>(revisions.keySet().stream().toList());
        this.sortedVersions.sort(Comparator.comparing(JiraVersion::releaseDate)); // Sort versions for release date
        this.metrics = new ArrayList<>();
        this.entryKeys = new HashMap<>();
        this.entryValues = new HashMap<>();

        mapInitialization();
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
        try {
            for (int i = 0; i < sortedVersions.size(); i++) {
                JiraVersion version = sortedVersions.get(i);
                GitCommitEntry revision = revisions.get(version);

                List<String> classList = entryKeys.get(i);
                for (String aClass : classList) {
                    String contents = git.getContentsOfClass(revision, aClass);
                    int loc = contents.split("\n").length;
                    entryValues.get(aClass).get(i).metrics().put(metric, Integer.toString(loc));
                }
            }
            metrics.add(metric);
        } catch (GitFileException e) {
            throw new MetricException(metric, "Could not get file contents", e);
        }
    }

    public void applyLOCTouchedMetric() throws MetricException {
        String metric = "LOC Touched";
        MetricController controller = new MetricController();
        controller.applyDifferenceMetric(metric, git, sortedVersions, revisions, entryKeys, entryValues, diff -> {
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
        controller.applyDifferenceMetric(metric, git, sortedVersions, revisions, entryKeys, entryValues, diff -> {
            int churn = 0;
            if (diff != null)
                churn = diff.added() - diff.deleted();
            return Integer.toString(churn);
        });
        metrics.add(metric);
    }

    public void writeToFile(String output) throws DatasetWriterException {
        WriterController controller = new WriterController();
        controller.writeToFile(output, sortedVersions, metrics, entryKeys, entryValues);
    }

    private void mapInitialization() throws GitLogException {
        // For every version
        for (int i = 0; i < sortedVersions.size(); i++) {
            // Get the revision
            GitCommitEntry revision = revisions.get(sortedVersions.get(i));
            // Get the classes of the revision
            List<String> classList = git.getClassList(revision);
            // Associate version to classes
            entryKeys.put(i, classList);
            // For every class
            for (String aClass : classList) {
                // Initialize the versions of the class
                List<DatasetEntry> entries = new ArrayList<>();
                for (int j = 0; j < sortedVersions.size(); j++)
                    entries.add(j, new DatasetEntry());

                // Associate the class to the dataset entries
                entryValues.put(aClass, entries);
            }
        }
    }
}

package it.uniroma2.alessandrolioi.dataset;

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
    private final Map<JiraVersion, List<String>> entryKeys;
    // Maps class name to its metrics
    private final Map<String, DatasetEntry> entryValues;
    private final List<String> metrics;

    public DatasetBuilder(Map<JiraVersion, GitCommitEntry> revisions, Git git) {
        this.revisions = revisions;
        this.git = git;
        this.sortedVersions = new ArrayList<>(revisions.keySet().stream().toList());
        this.sortedVersions.sort(Comparator.comparing(JiraVersion::releaseDate)); // Sort versions for release date
        this.metrics = new ArrayList<>();
        this.entryKeys = new HashMap<>();
        this.entryValues = new HashMap<>();
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
            for (JiraVersion version : sortedVersions) {
                GitCommitEntry revision = revisions.get(version);

                // Maps initialization
                List<String> classList = new ArrayList<>();
                if (entryKeys.containsKey(version)) {
                    // It's not the first metric calculated for this version, so it already contains the class list
                    classList = entryKeys.get(version);
                } else {
                    // First metric to calculate, so the class list has to be retrieved from the revision
                    classList = git.getClassList(revision);
                    // Map key initialization
                    entryKeys.put(version, classList);
                    // Map values initialization
                    for (String aClass : classList) {
                        entryValues.put(aClass, new DatasetEntry(new HashMap<>(), false));
                    }
                }

                for (String aClass : classList) {
                    String contents = git.getContentsOfClass(revision, aClass);
                    int loc = contents.split("\n").length;
                    entryValues.get(aClass).metrics().put(metric, Integer.toString(loc));
                }
            }
            metrics.add(metric);
        } catch (GitFileException e) {
            throw new MetricException(metric, "Could not get file contents", e);
        } catch (GitLogException e) {
            throw new MetricException(metric, "Could not get class list", e);
        }
    }

    public void writeToFile(String output) throws DatasetWriterException {
        WriterController controller = new WriterController();
        controller.writeToFile(output, sortedVersions, metrics, entryKeys, entryValues);
    }
}

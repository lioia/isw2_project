package it.uniroma2.alessandrolioi.dataset;

import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.dataset.controllers.BuggyController;
import it.uniroma2.alessandrolioi.dataset.controllers.MetricController;
import it.uniroma2.alessandrolioi.dataset.controllers.WriterController;
import it.uniroma2.alessandrolioi.dataset.exceptions.BuggyException;
import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.exceptions.MetricException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.dataset.models.MetricValue;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.JiraGitIntegration;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;

public class DatasetBuilder {
    // Sorted list of Jira releases
    private final List<Pair<JiraVersion, GitCommitEntry>> versions;
    private final Map<JiraIssue, GitCommitEntry> issues;

    // Provides functionality to explore the git repository to calculate metrics
    private final Git git;

    // Maps class name to its metrics. i-th element in the list is the entry for the i-th version
    private final Map<String, List<DatasetEntry>> entries;
    // Metrics used in dataset
    private final List<String> metrics;

    public DatasetBuilder(JiraGitIntegration integration, Git git) {
        this.git = git;
        this.metrics = new ArrayList<>();
        this.entries = new HashMap<>();
        this.issues = integration.issues();
        this.versions = integration.versions();

        // Initialize `revisions`
        for (Pair<JiraVersion, GitCommitEntry> version : this.versions) {
            GitCommitEntry revision = version.second();

            // Initialize `entries` map
            for (String aClass : revision.classList()) {
                List<DatasetEntry> datasetEntries = new ArrayList<>();
                for (int i = 0; i < this.versions.size(); i++)
                    datasetEntries.add(i, new DatasetEntry());
                entries.put(aClass, datasetEntries);
            }
        }
    }

    private void applyLOCMetric() throws MetricException {
        MetricController controller = new MetricController();
        controller.applyLOCMetric(git, versions, this::applyMetric);
        metrics.add("LOC");
    }

    // LOC Touched, Churn
    private void applyDifferenceMetrics() throws MetricException {
        // Initialize first release
        for (String aClass : versions.get(0).second().classList()) {
            for (DatasetEntry entry : entries.get(aClass)) {
                entry.metrics().put("LOC Touched", "0");
                entry.metrics().put("Churn", "0");
            }
        }
        MetricController controller = new MetricController();
        controller.applyDifferenceMetric(git, versions, this::applyMetric);
        metrics.addAll(List.of("LOC Touched", "Churn"));
    }

    // Average LOC Added, Max LOC Added, Average Churn, Max Churn
    private void applyCumulativeMetrics() throws MetricException {
        // Initialize first release
        for (String aClass : versions.get(0).second().classList()) {
            for (DatasetEntry entry : entries.get(aClass)) {
                entry.metrics().put("Average LOC Added", "0");
                entry.metrics().put("Max LOC Added", "0");
                entry.metrics().put("Average Churn", "0");
                entry.metrics().put("Max Churn", "0");
            }
        }
        MetricController controller = new MetricController();
        controller.applyCumulativeMetric(git, versions, this::applyMetric);
        metrics.addAll(List.of("Average LOC Added", "Max LOC Added", "Average Churn", "Max Churn"));
    }

    // NR and Age
    public void applyListMetrics() throws MetricException {
        // Initialize first release
        for (String aClass : versions.get(0).second().classList()) {
            for (DatasetEntry entry : entries.get(aClass)) {
                entry.metrics().put("NR", "0");
                entry.metrics().put("Age", "0");
                entry.metrics().put("NFix", "0");
            }
        }
        MetricController controller = new MetricController();
        controller.applyListMetric(git, versions, issues, this::applyMetric);
        metrics.addAll(List.of("NR", "Age", "NFix"));
    }

    public void setBuggy(int lastVersion) throws BuggyException {
        BuggyController controller = new BuggyController();
        List<Pair<JiraVersion, GitCommitEntry>> subList = versions.subList(0, lastVersion); // maybe + 1
        controller.calculateBuggy(git, subList, issues, buggy -> {
            buggy.first().stream().filter(entries::containsKey).forEach(aClass -> {
                for (int version : buggy.second()) {
                    entries.get(aClass).get(version).setBuggy(true);
                }
            });
            return null;
        });
    }

    public void applyMetrics() throws MetricException {
        applyLOCMetric();
        applyDifferenceMetrics();
        applyCumulativeMetrics();
        applyListMetrics();
    }

    public void writeToFile(String output, int numberOfVersions) throws DatasetWriterException {
        WriterController controller = new WriterController();
        controller.writeToFile(output, versions, metrics, entries, numberOfVersions);
    }

    private Void applyMetric(MetricValue metric) {
        entries
                .get(metric.aClass())
                .get(metric.version())
                .metrics()
                .put(metric.metric(), String.valueOf(metric.value()));
        return null;
    }
}

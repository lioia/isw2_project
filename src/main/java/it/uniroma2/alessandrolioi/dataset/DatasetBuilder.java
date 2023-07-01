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

    public DatasetBuilder(JiraGitIntegration integration, Git git) {
        this.git = git;
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

    public void applyMetrics() throws MetricException {
        MetricController controller = new MetricController();
        controller.applyLOCMetric(git, versions, this::applyMetric);
        controller.applyDifferenceMetric(git, versions, this::applyMetric);
        controller.applyCumulativeMetric(git, versions, this::applyMetric);
        controller.applyListMetric(git, versions, issues, this::applyMetric);
    }

    public void setBuggy(int lastVersion) throws BuggyException {
        BuggyController controller = new BuggyController();
        List<Pair<JiraVersion, GitCommitEntry>> subList = versions.subList(0, lastVersion);
        controller.calculateBuggy(git, subList, issues, buggy -> {
            buggy.first().stream().filter(entries::containsKey).forEach(aClass -> {
                for (int version : buggy.second())
                    entries.get(aClass).get(version).setBuggy(true);
            });
            return null;
        });
    }

    public void writeToFile(String project, int numberOfVersions) throws DatasetWriterException {
        WriterController controller = new WriterController();
        String text = controller.writeToText(versions, entries, numberOfVersions);
        controller.writeToFile(project, text, String.valueOf(numberOfVersions));
    }

    public void writeOracle(String project, int numberOfVersions) throws DatasetWriterException {
        WriterController controller = new WriterController();
        String text = controller.writeToText(versions, entries, numberOfVersions);
        controller.writeToFile(project, text, "oracle");
    }

    private Void applyMetric(MetricValue metric) {
        entries.get(metric.aClass())
                .get(metric.version())
                .metrics()
                .put(metric.metric(), String.valueOf(metric.value()));
        return null;
    }
}

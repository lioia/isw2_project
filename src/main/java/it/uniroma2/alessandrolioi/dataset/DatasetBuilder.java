package it.uniroma2.alessandrolioi.dataset;

import it.uniroma2.alessandrolioi.dataset.controllers.MetricController;
import it.uniroma2.alessandrolioi.dataset.controllers.WriterController;
import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.exceptions.MetricException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.JiraGitIntegration;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

public class DatasetBuilder {
    // Sorted list of Jira releases
    private final List<JiraVersion> versions;
    // Sorted list of revisions corresponding to a Jira release
    private final List<GitCommitEntry> revisions;

    // Integration classes used to calculate the metrics
    private final JiraGitIntegration integration;
    private final Git git;

    // Maps class name to its metrics. i-th element in the list is the entry for the i-th version
    private final Map<String, List<DatasetEntry>> entries;
    // Metrics used in dataset
    private final List<String> metrics;

    public DatasetBuilder(JiraGitIntegration integration, Git git) {
        this.revisions = new ArrayList<>();
        this.git = git;
        this.metrics = new ArrayList<>();
        this.entries = new HashMap<>();
        this.integration = integration;

        // Sort versions by release date
        this.versions = new ArrayList<>(integration.revisions().keySet().stream().toList());
        this.versions.sort(Comparator.comparing(JiraVersion::releaseDate)); // Sort versions for release date

        // Initialize `revisions`
        for (JiraVersion version : this.versions) {
            GitCommitEntry revision = integration.revisions().get(version);

            // Initialize `entries` map
            for (String aClass : revision.classList()) {
                List<DatasetEntry> datasetEntries = new ArrayList<>();
                for (int i = 0; i < this.versions.size(); i++)
                    datasetEntries.add(i, new DatasetEntry());
                entries.put(aClass, datasetEntries);
            }

            revisions.add(revision);
        }
    }

    private void applyLOCMetric() throws MetricException {
        String metric = "LOC";
        MetricController controller = new MetricController();
        controller.applyLOCMetric(metric, git, revisions, entries);
        metrics.add(metric);
    }

    // LOC Touched, Churn
    private void applyDifferenceMetrics() throws MetricException {
        // Initialize first release
        for (String aClass : revisions.get(0).classList()) {
            for (DatasetEntry entry : entries.get(aClass)) {
                entry.metrics().put("LOC Touched", "0");
                entry.metrics().put("Churn", "0");
            }
        }
        MetricController controller = new MetricController();
        controller.applyDifferenceMetric(git, revisions, entries);
        metrics.addAll(List.of("LOC Touched", "Churn"));
    }

    // Average LOC Added, Max LOC Added, Average Churn, Max Churn
    private void applyCumulativeMetrics() throws MetricException {
        // Initialize first release
        for (String aClass : revisions.get(0).classList()) {
            for (DatasetEntry entry : entries.get(aClass)) {
                entry.metrics().put("Average LOC Added", "0");
                entry.metrics().put("Max LOC Added", "0");
                entry.metrics().put("Average Churn", "0");
                entry.metrics().put("Max Churn", "0");
            }
        }
        MetricController controller = new MetricController();
        controller.applyCumulativeMetric(git, revisions, entries);
        metrics.addAll(List.of("Average LOC Added", "Max LOC Added", "Average Churn", "Max Churn"));
    }


    // NR and Age
    public void applyListMetrics() throws MetricException {
        // Initialize first release
        for (String aClass : revisions.get(0).classList()) {
            for (DatasetEntry entry : entries.get(aClass)) {
                entry.metrics().put("NR", "0");
                entry.metrics().put("Age", "0");
            }
        }
        MetricController controller = new MetricController();
        controller.applyListMetric(git, revisions, info -> {
            // Age calculated as ((lastCommitTime - firstCommitTime) / (currentReleaseTime - lastReleaseTime))
            double age = 0.0;
            if (!info.commits().isEmpty()) {
                info.commits().sort(Comparator.comparing(GitCommitEntry::commitDate));
                LocalDateTime first = info.commits().get(0).commitDate();
                LocalDateTime last = info.commits().get(info.commits().size() - 1).commitDate();
                double totalTime = (double) info.first().commitDate().toEpochSecond(ZoneOffset.UTC) - info.second().commitDate().toEpochSecond(ZoneOffset.UTC);
                double time = (double) last.toEpochSecond(ZoneOffset.UTC) - first.toEpochSecond(ZoneOffset.UTC);
                age = Math.abs(time / totalTime);
            }
            entries.get(info.aClass()).get(info.versionIndex()).metrics().put("NR", String.valueOf(info.commits().size()));
            entries.get(info.aClass()).get(info.versionIndex()).metrics().put("Age", String.valueOf(age));
            return null;
        });
        metrics.addAll(List.of("NR", "Age"));
    }

    // NFix and set Buggy
    public void applyTicketsMetric() throws MetricException {
        // Initialize first release
        for (String aClass : revisions.get(0).classList())
            for (DatasetEntry entry : entries.get(aClass))
                entry.metrics().put("NFix", "0");
        MetricController controller = new MetricController();
        controller.applyListMetric(git, revisions, info -> {
            // Get the current version
            JiraVersion version = versions.get(info.versionIndex());
            // Get the hashes of the commits between two releases
            List<String> commits = info.commits().stream().map(GitCommitEntry::hash).toList();
            // Get the hashes of the commits relating to the fixed issues of this version
            Stream<String> fixed = version.fixed().stream().map(i -> integration.issues().get(i).hash());
            // Get the intersection of this two list
            List<String> common = fixed.filter(commits::contains).toList();
            // Save the size of the list
            entries.get(info.aClass()).get(info.versionIndex()).metrics().put("NFix", String.valueOf(common.size()));
            if(!common.isEmpty()) entries.get(info.aClass()).get(info.versionIndex()).setBuggy(true);
            return null;
        });
        metrics.add("NFix");
    }

    public void applyMetrics() throws MetricException {
        applyLOCMetric();
        applyDifferenceMetrics();
        applyCumulativeMetrics();
        applyListMetrics();
        applyTicketsMetric();
    }

    public void writeToFile(String output) throws DatasetWriterException {
        WriterController controller = new WriterController();
        controller.writeToFile(output, revisions, metrics, entries);
    }
}

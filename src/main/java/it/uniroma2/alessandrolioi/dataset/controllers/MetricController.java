package it.uniroma2.alessandrolioi.dataset.controllers;

import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.dataset.exceptions.MetricException;
import it.uniroma2.alessandrolioi.common.Metric;
import it.uniroma2.alessandrolioi.dataset.models.MetricValue;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.exceptions.GitFileException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetricController {

    public void applyLOCMetric(Git git, List<Pair<JiraVersion, GitCommitEntry>> versions,
                               Function<MetricValue, Void> func) throws MetricException {
        try {
            // For every revision
            for (int i = 0; i < versions.size(); i++) {
                GitCommitEntry revision = versions.get(i).second();

                // For every class
                for (String aClass : revision.classList()) {
                    // Calculate the LOC of a file calculating the number of lines
                    String contents = git.getContentsOfClass(revision, aClass);
                    int loc = contents.split("\n").length;
                    MetricValue value = new MetricValue(aClass, i, Metric.LOC, loc);
                    func.apply(value);
                }
            }
        } catch (GitFileException e) {
            throw new MetricException(e);
        }
    }

    public void applyDifferenceMetric(Git git, List<Pair<JiraVersion, GitCommitEntry>> versions,
                                      Function<MetricValue, Void> func) throws MetricException {
        try {
            // For every pair of consecutive releases
            GitCommitEntry previous = git.getFirstCommit();
            // For every consecutive pair of classes
            for (int i = 0; i < versions.size(); i++) {
                GitCommitEntry current = versions.get(i).second();
                // Get the differences between commits
                Map<String, GitDiffEntry> diffs = git.getDifferences(previous, current);

                // For every class in the current release
                for (String aClass : current.classList()) {
                    // Get the diff of this class
                    GitDiffEntry diff = diffs.get(aClass);
                    // Calculate the LOC touched and the churn
                    int locTouched = 0;
                    int churn = 0;
                    if (diff != null) {
                        locTouched = diff.touched();
                        churn = diff.churn();
                    }
                    MetricValue locTouchedMetric = new MetricValue(aClass, i, Metric.LOC_TOUCHED, locTouched);
                    MetricValue churnMetric = new MetricValue(aClass, i, Metric.CHURN, churn);
                    func.apply(locTouchedMetric);
                    func.apply(churnMetric);
                }

                // Set previous version as the current for the next iteration
                previous = current;
            }
        } catch (GitDiffException e) {
            throw new MetricException(e);
        } catch (GitLogException e) {
            throw new MetricException("Could not get first commit", e);
        }
    }

    public void applyCumulativeMetric(Git git, List<Pair<JiraVersion, GitCommitEntry>> versions,
                                      Function<MetricValue, Void> func) throws MetricException {
        try {
            GitCommitEntry previous = git.getFirstCommit();

            // For every consecutive pair of versions
            for (int i = 0; i < versions.size(); i++) {
                GitCommitEntry current = versions.get(i).second();
                // For every class
                for (String aClass : current.classList()) {
                    // Get all the incremental differences of the class between the releases
                    List<GitDiffEntry> diffs = git.getAllDifferencesOfClass(previous, current, aClass);
                    // Size of the `diffs` list (set as 1 if it's empty, so there's not dividing-by-zero error)
                    int size = diffs.size();
                    if (diffs.isEmpty()) size = 1;
                    // Calculating the max LOC added
                    int maxLocAdded = diffs.stream().map(GitDiffEntry::added).max(Comparator.naturalOrder()).orElse(0);
                    MetricValue maxLocAddedMetric = new MetricValue(aClass, i, Metric.MAX_LOC_ADDED, maxLocAdded);
                    func.apply(maxLocAddedMetric);
                    // Calculating the max Churn
                    int maxChurn = diffs.stream().map(GitDiffEntry::churn).max(Comparator.naturalOrder()).orElse(0);
                    MetricValue maxChurnMetric = new MetricValue(aClass, i, Metric.MAX_CHURN, maxChurn);
                    func.apply(maxChurnMetric);
                    // Calculating the sum of all the LOC added
                    int sumLocAdded = diffs.stream().map(GitDiffEntry::added).reduce(Integer::sum).orElse(0);
                    MetricValue averageLocAddedMetric = new MetricValue(aClass, i, Metric.AVERAGE_LOC_ADDED, sumLocAdded / size);
                    func.apply(averageLocAddedMetric);
                    // Calculating the sum of all Churn
                    int sumChurn = diffs.stream().map(GitDiffEntry::churn).reduce(Integer::sum).orElse(0);
                    MetricValue averageChurnMetric = new MetricValue(aClass, i, Metric.AVERAGE_CHURN, sumChurn / size);
                    func.apply(averageChurnMetric);
                }
                // Set previous version as the current for the next iteration
                previous = current;
            }
        } catch (GitDiffException | GitLogException e) {
            throw new MetricException(e);
        }
    }

    public void applyListMetric(Git git, List<Pair<JiraVersion, GitCommitEntry>> versions,
                                Map<JiraIssue, GitCommitEntry> issues,
                                Function<MetricValue, Void> func) throws MetricException {
        try {
            GitCommitEntry previous = git.getFirstCommit();
            // For every consecutive pair of versions
            for (int i = 0; i < versions.size(); i++) {
                Pair<JiraVersion, GitCommitEntry> current = versions.get(i);
                // For every class
                for (String aClass : current.second().classList()) {
                    // Get every commit between two releases
                    List<GitCommitEntry> commits = git.getAllCommitsOfClass(previous, current.second(), aClass);
                    // NR
                    MetricValue nrMetric = new MetricValue(aClass, i, Metric.NR, commits.size());
                    func.apply(nrMetric);

                    // NAuth
                    int numberOfAuthors = commits.stream().map(GitCommitEntry::author).collect(Collectors.toSet()).size();
                    MetricValue nAuthMetric = new MetricValue(aClass, i, Metric.N_AUTH, numberOfAuthors);
                    func.apply(nAuthMetric);

                    // NFix
                    List<String> hashes = new ArrayList<>(commits.stream().map(GitCommitEntry::hash).toList());
                    hashes.addAll(List.of(previous.hash(), current.second().hash()));
                    long nFix = current.first().fixed().stream()
                            .filter(issue -> hashes.contains(issues.get(issue).hash())) // Fixed issues contained in this commit range
                            .count();
                    MetricValue nFixMetric = new MetricValue(aClass, i, Metric.N_FIX, nFix);
                    func.apply(nFixMetric);
                }
                // Set previous version as the current for the next iteration
                previous = current.second();
            }
        } catch (GitLogException e) {
            throw new MetricException(e);
        }
    }
}

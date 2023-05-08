package it.uniroma2.alessandrolioi.dataset.controllers;

import it.uniroma2.alessandrolioi.dataset.exceptions.MetricException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.dataset.models.RevisionPairInfo;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.exceptions.GitFileException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MetricController {

    public void applyLOCMetric(String metric, Git git, List<GitCommitEntry> revisions, Map<String, List<DatasetEntry>> entries) throws MetricException {
        try {
            for (int i = 0; i < revisions.size(); i++) {
                GitCommitEntry revision = revisions.get(i);

                for (String aClass : revision.classList()) {
                    String contents = git.getContentsOfClass(revision, aClass);
                    int loc = contents.split("\n").length;
                    entries.get(aClass).get(i).metrics().put(metric, Integer.toString(loc));
                }
            }
        } catch (GitFileException e) {
            throw new MetricException("Could not get file contents", e);
        }
    }

    public void applyDifferenceMetric(Git git, List<GitCommitEntry> revisions, Map<String, List<DatasetEntry>> entries) throws MetricException {
        try {
            GitCommitEntry previous = revisions.get(0);
            // For every consecutive pair of classes
            for (int i = 1; i < revisions.size(); i++) {
                GitCommitEntry current = revisions.get(i);
                // Get the differences between commits
                Map<String, GitDiffEntry> diffs = git.getDifferences(previous, current);

                // For every class in the current release
                for (String aClass : current.classList()) {
                    GitDiffEntry diff = diffs.get(aClass);
                    int locTouched = 0;
                    int churn = 0;
                    if (diff != null) {
                        locTouched = diff.touched();
                        churn = diff.churn();
                    }
                    entries.get(aClass).get(i).metrics().put("LOC Touched", String.valueOf(locTouched));
                    entries.get(aClass).get(i).metrics().put("Churn", String.valueOf(churn));
                }

                // Set previous version as the current for the next iteration
                previous = current;
            }
        } catch (GitDiffException e) {
            throw new MetricException("Could not load differences between commits", e);
        }
    }

    public void applyCumulativeMetric(Git git, List<GitCommitEntry> revisions, Map<String, List<DatasetEntry>> entries) throws MetricException {
        try {
            GitCommitEntry previous = revisions.get(0);

            // For every consecutive pair of versions
            for (int i = 1; i < revisions.size(); i++) {
                GitCommitEntry current = revisions.get(i);
                for (String aClass : current.classList()) {
                    List<GitDiffEntry> diffs = git.getAllDifferencesOfClass(previous, current, aClass);
                    int size = diffs.size();
                    if (diffs.isEmpty()) size = 1;
                    int maxLocAdded = diffs.stream().map(GitDiffEntry::added).max(Comparator.naturalOrder()).orElse(0);
                    entries.get(aClass).get(i).metrics().put("Max LOC Added", String.valueOf(maxLocAdded));
                    int maxChurn = diffs.stream().map(GitDiffEntry::churn).max(Comparator.naturalOrder()).orElse(0);
                    entries.get(aClass).get(i).metrics().put("Max Churn", String.valueOf(maxChurn));
                    int sumLocAdded = diffs.stream().map(GitDiffEntry::added).reduce(Integer::sum).orElse(0);
                    entries.get(aClass).get(i).metrics().put("Average LOC Added", String.valueOf(sumLocAdded / size));
                    int sumChurn = diffs.stream().map(GitDiffEntry::churn).reduce(Integer::sum).orElse(0);
                    entries.get(aClass).get(i).metrics().put("Average Churn", String.valueOf(sumChurn / size));
                }
                previous = current;
            }
        } catch (GitDiffException e) {
            throw new MetricException("Could not load differences", e);
        }
    }

    public void applyListMetric(Git git, List<GitCommitEntry> revisions,
                                Map<String, List<DatasetEntry>> entries,
                                Function<RevisionPairInfo, Void> func) throws MetricException {
        try {
            GitCommitEntry previous = revisions.get(0);
            // For every consecutive pair of versions
            for (int i = 1; i < revisions.size(); i++) {
                GitCommitEntry current = revisions.get(i);
                for (String aClass : current.classList()) {
                    List<GitCommitEntry> commits = git.getAllCommitsOfClass(previous, current, aClass);
                    RevisionPairInfo info = new RevisionPairInfo(previous, current, i, aClass, commits);
                    func.apply(info);
                }
                previous = current;
            }
        } catch (GitLogException e) {
            throw new MetricException("Could not load commits", e);
        }
    }
}

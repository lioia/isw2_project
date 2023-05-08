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
            throw new MetricException(metric, "Could not get file contents", e);
        }
    }

    public void applyDifferenceMetric(String metric, Git git, List<GitCommitEntry> revisions,
                                      Map<String, List<DatasetEntry>> entries,
                                      Function<GitDiffEntry, String> func) throws MetricException {
        try {
            GitCommitEntry previous = revisions.get(0);
            // First version cannot calculate this metric,
            // because it does not have a previous revision that it can compare it to
            for (String aClass : previous.classList())
                entries.get(aClass).get(0).metrics().put(metric, "0");

            // For every consecutive pair of classes
            for (int i = 1; i < revisions.size(); i++) {
                GitCommitEntry current = revisions.get(i);
                // Get the differences between commits
                Map<String, GitDiffEntry> diffs = git.getDifferences(previous, current);

                // For every class in the current release
                for (String aClass : current.classList()) {
                    GitDiffEntry diff = diffs.get(aClass);
                    String result = func.apply(diff);
                    entries.get(aClass).get(i).metrics().put(metric, result);
                }

                // Set previous version as the current for the next iteration
                previous = current;
            }
        } catch (GitDiffException e) {
            throw new MetricException(metric, "Could not load differences between commits", e);
        }
    }

    public void applyCumulativeMetric(String metric, Git git, List<GitCommitEntry> revisions,
                                      Map<String, List<DatasetEntry>> entries,
                                      Function<List<GitDiffEntry>, String> func) throws MetricException {
        try {
            GitCommitEntry previous = revisions.get(0);
            // First version cannot calculate this metric,
            // because it does not have a previous revision that it can compare it to
            for (String aClass : revisions.get(0).classList())
                entries.get(aClass).get(0).metrics().put(metric, "0");

            // For every consecutive pair of versions
            for (int i = 1; i < revisions.size(); i++) {
                GitCommitEntry current = revisions.get(i);
                for (String aClass : current.classList()) {
                    List<GitDiffEntry> diffs = git.getAllDifferencesOfClass(previous, current, aClass);
                    String result = func.apply(diffs);
                    entries.get(aClass).get(i).metrics().put(metric, result);
                }
                previous = current;
            }
        } catch (GitDiffException e) {
            throw new MetricException(metric, "Could not load differences", e);
        }
    }

    public void applyListMetric(String metric, Git git, List<GitCommitEntry> revisions,
                                Map<String, List<DatasetEntry>> entries,
                                Function<RevisionPairInfo, String> func) throws MetricException {
        try {
            GitCommitEntry previous = revisions.get(0);
            // First version cannot calculate this metric,
            // because it does not have a previous revision that it can compare it to
            for (String aClass : revisions.get(0).classList())
                entries.get(aClass).get(0).metrics().put(metric, "0");

            // For every consecutive pair of versions
            for (int i = 1; i < revisions.size(); i++) {
                GitCommitEntry current = revisions.get(i);
                for (String aClass : current.classList()) {
                    List<GitCommitEntry> commits = git.getAllCommitsOfClass(previous, current, aClass);
                    RevisionPairInfo info = new RevisionPairInfo(previous, current, commits);
                    String result = func.apply(info);
                    entries.get(aClass).get(i).metrics().put(metric, result);
                }
                previous = current;
            }
        } catch (GitLogException e) {
            throw new RuntimeException(e);
        }
    }
}

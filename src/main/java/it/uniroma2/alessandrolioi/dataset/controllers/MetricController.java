package it.uniroma2.alessandrolioi.dataset.controllers;

import it.uniroma2.alessandrolioi.dataset.exceptions.MetricException;
import it.uniroma2.alessandrolioi.dataset.models.DatasetEntry;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MetricController {
    public void applyDifferenceMetric(String metric, Git git, List<JiraVersion> versions,
                                      Map<JiraVersion, GitCommitEntry> revisions, Map<Integer, List<String>> entryKeys,
                                      Map<String, List<DatasetEntry>> entryValues, Function<GitDiffEntry, String> func) throws MetricException {
        try {
            JiraVersion previousVersion = versions.get(0);
            // First version cannot calculate this metric,
            // because it does not have a previous revision that it can compare it to
            for (String aClass : entryKeys.get(0))
                entryValues.get(aClass).get(0).metrics().put(metric, "0");

            // For every consecutive pair of classes
            for (int i = 1; i < versions.size(); i++) {
                JiraVersion currentVersion = versions.get(i);
                // Get the revisions of the previous and current release
                GitCommitEntry first = revisions.get(previousVersion);
                GitCommitEntry second = revisions.get(currentVersion);

                // Get the differences between commits
                Map<String, GitDiffEntry> diffs = git.getDifferences(first, second);

                // For every class in the current release
                for (String aClass : entryKeys.get(i)) {
                    GitDiffEntry diff = diffs.get(aClass);
                    String result = func.apply(diff);
                    entryValues.get(aClass).get(i).metrics().put(metric, result);
                }

                // Set previous version as the current for the next iteration
                previousVersion = currentVersion;
            }
        } catch (GitDiffException e) {
            throw new MetricException(metric, "Could not load differences between commits", e);
        }
    }
}

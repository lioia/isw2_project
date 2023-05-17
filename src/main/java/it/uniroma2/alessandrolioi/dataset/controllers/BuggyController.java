package it.uniroma2.alessandrolioi.dataset.controllers;

import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.dataset.exceptions.BuggyException;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

public class BuggyController {
    public void calculateBuggy(Git git, List<Pair<JiraVersion, GitCommitEntry>> versions,
                               Map<JiraIssue, GitCommitEntry> issues,
                               Function<Pair<List<String>, int[]>, Void> func) throws BuggyException {
        try {
            // For every version (after the first)
            for (int i = 1; i < versions.size(); i++) {
                Pair<JiraVersion, GitCommitEntry> current = versions.get(i);
                // For every issue fixed in this version
                for (JiraIssue fixedIssue : current.first().fixed()) {
                    GitCommitEntry fixedCommit = issues.get(fixedIssue);
                    List<String> modifiedClasses = git.getModifiedClassesOfCommit(fixedCommit);
                    int[] range = IntStream.range(fixedIssue.getIvIndex(), fixedIssue.getFvIndex()).toArray();
                    func.apply(new Pair<>(modifiedClasses, range));
                }
            }
        } catch (GitDiffException e) {
            throw new BuggyException("Could not load differences", e);
        }
    }
}
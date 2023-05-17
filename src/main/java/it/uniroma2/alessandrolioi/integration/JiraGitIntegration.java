package it.uniroma2.alessandrolioi.integration;

import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.controllers.FilterController;
import it.uniroma2.alessandrolioi.integration.exceptions.NotFoundException;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;

public class JiraGitIntegration {
    private final List<GitCommitEntry> commits;
    private final List<Pair<JiraVersion, GitCommitEntry>> versions;
    private final Map<JiraIssue, GitCommitEntry> issues;

    public JiraGitIntegration(List<GitCommitEntry> commits) {
        this.commits = commits;
        this.versions = new ArrayList<>();
        this.issues = new HashMap<>();
    }

    public void findRevisions(List<JiraVersion> versions) throws NotFoundException {
        for (JiraVersion version : versions) {
            GitCommitEntry revisionVersion = findRevisionOfVersion(version);
            this.versions.add(new Pair<>(version, revisionVersion));

            for (JiraIssue issue : version.fixed()) {
                GitCommitEntry revisionIssue = findRevisionOfIssue(issue);
                this.issues.put(issue, revisionIssue);
            }
        }
    }

    private GitCommitEntry findRevisionOfVersion(JiraVersion version) throws NotFoundException {
        FilterController filter = new FilterController();
        GitCommitEntry candidate;
        try {
            candidate = filter.useSemanticFilter(version.name(), commits);
        } catch (NotFoundException e) {
            candidate = filter.useDateFilter(version.releaseDate(), commits);
        }
        return candidate;
    }

    private GitCommitEntry findRevisionOfIssue(JiraIssue issue) throws NotFoundException {
        FilterController filter = new FilterController();
        GitCommitEntry candidate;
        try {
            candidate = filter.useSemanticKeyFilter(issue.getKey(), commits);
        } catch (NotFoundException e) {
            candidate = filter.useDateFilter(issue.getResolution(), commits);
        }
        return candidate;
    }

    public List<Pair<JiraVersion, GitCommitEntry>> versions() {
        return versions;
    }

    public Map<JiraIssue, GitCommitEntry> issues() {
        return issues;
    }
}

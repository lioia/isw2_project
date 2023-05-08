package it.uniroma2.alessandrolioi.integration;

import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.controllers.FilterController;
import it.uniroma2.alessandrolioi.integration.exceptions.NotFoundException;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;

public class JiraGitIntegration {
    private final List<GitCommitEntry> commits;
    private final Map<JiraVersion, GitCommitEntry> revisions;
    private final Map<JiraIssue, GitCommitEntry> issues;

    public JiraGitIntegration(List<GitCommitEntry> commits) {
        this.commits = commits;
        this.revisions = new HashMap<>();
        this.issues = new HashMap<>();
    }

    public void findRevisions(List<JiraVersion> versions) throws NotFoundException {
        for (JiraVersion version : versions) {
            revisions.put(version, findRevisionOfVersion(version));

            for (JiraIssue issue : version.fixed())
                issues.put(issue, findRevisionOfIssue(issue));
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

    public Map<JiraVersion, GitCommitEntry> revisions() {
        return revisions;
    }

    public Map<JiraIssue, GitCommitEntry> issues() {
        return issues;
    }
}

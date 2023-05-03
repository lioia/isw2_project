package it.uniroma2.alessandrolioi.integration;

import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.exceptions.NotFoundException;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;

public class JiraGitIntegration {
    public Map<JiraVersion, GitCommitEntry> findRevisionsOfVersions(List<JiraVersion> versions, List<GitCommitEntry> commits) throws NotFoundException {
        FilterController filter = new FilterController();
        Map<JiraVersion, GitCommitEntry> revisions = new HashMap<>();
        for (JiraVersion version : versions) {
            GitCommitEntry candidate = null;
            try {
                candidate = filter.useSemanticFilter(version.name(), commits);
            } catch (NotFoundException e) {
                candidate = filter.useDateFilter(version.releaseDate(), commits);
            } finally {
                revisions.put(version, candidate);
            }
        }
        return revisions;
    }

    public GitCommitEntry findRevisionOfIssue(JiraIssue issue, List<GitCommitEntry> commits) throws NotFoundException {
        FilterController filter = new FilterController();
        GitCommitEntry candidate;
        try {
            candidate = filter.useSemanticKeyFilter(issue.getKey(), commits);
        } catch (NotFoundException e) {
            candidate = filter.useDateFilter(issue.getResolution(), commits);
        }
        return candidate;
    }
}

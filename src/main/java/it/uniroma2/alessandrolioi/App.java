package it.uniroma2.alessandrolioi;

import it.uniroma2.alessandrolioi.git.controller.GitRepo;
import it.uniroma2.alessandrolioi.git.exceptions.GitException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.Jira;
import it.uniroma2.alessandrolioi.jira.exceptions.JiraRESTException;
import it.uniroma2.alessandrolioi.jira.models.JiraCompleteIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.List;
import java.util.Objects;

public class App {
    public static void main(String[] args) {
        GitRepo repo = null;
        try {
            repo = new GitRepo("bookkeeper", "https://github.com/apache/bookkeeper", "master");
            List<GitCommitEntry> commits = repo.getCommits();
            commits.forEach(c -> System.out.println(c.hash()));
        } catch (GitException e) {
            throw new RuntimeException(e);
        } finally {
            Objects.requireNonNull(repo).clean();
        }

        Jira jira = new Jira("bookkeeper");
        try {
            List<JiraVersion> versions = jira.loadVersions();
            List<JiraIssue> issues = jira.loadIssues();
            List<JiraCompleteIssue> completeIssues = jira.getCompleteIssues(versions, issues);
            for (JiraCompleteIssue i : completeIssues) {
                String injected = i.getInjected() != null ? i.getInjected().name() : "x.x.x";
                System.out.printf("%s | IV: %s, OV: %s, FV: %s | FV-IV: %d, FV-OV: %d%n", i.getKey(), injected, i.getOpening().name(), i.getFix().name(), i.getFvIvDifference(), i.getFvOvDifference());
            }
        } catch (JiraRESTException e) {
            throw new RuntimeException(e);
        }
    }
}

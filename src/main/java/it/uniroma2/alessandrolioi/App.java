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

        Jira bookkeeper = new Jira("bookkeeper");
        try {
            List<JiraVersion> bookkeeperVersions = bookkeeper.loadVersions();
            List<JiraIssue> bookkeeperIssues = bookkeeper.loadIssues(bookkeeperVersions.get(0).releaseDate(), bookkeeperVersions.get(bookkeeperVersions.size() - 1).releaseDate());
            List<JiraCompleteIssue> bookkeeperCompleteIssues = bookkeeper.getCompleteIssues(bookkeeperVersions, bookkeeperIssues);
            double avroColdStart = calculateColdStart("avro");
            bookkeeper.applyProportionIncrement(bookkeeperCompleteIssues, bookkeeperVersions, avroColdStart);
            for (JiraCompleteIssue i : bookkeeperCompleteIssues) {
                String injected = i.getInjected() != null ? i.getInjected().name() : "x.x.x";
                System.out.printf("%s | IV: %s, OV: %s, FV: %s | FV-IV: %d, FV-OV: %d%n", i.getKey(), injected, i.getOpening().name(), i.getFix().name(), i.getFvIvDifference(), i.getFvOvDifference());
            }
        } catch (JiraRESTException e) {
            throw new RuntimeException(e);
        }
    }

    private static double calculateColdStart(String project) throws JiraRESTException {
        Jira avro = new Jira(project);
        List<JiraVersion> avroVersions = avro.loadVersions();
        List<JiraIssue> avroIssues = avro.loadIssues(avroVersions.get(0).releaseDate(), avroVersions.get(avroVersions.size() - 1).releaseDate());
        List<JiraCompleteIssue> avroCompleteIssues = avro.getCompleteIssues(avroVersions, avroIssues);
        return avro.calculateProportionColdStart(avroCompleteIssues);
    }
}

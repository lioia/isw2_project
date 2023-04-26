package it.uniroma2.alessandrolioi;

import it.uniroma2.alessandrolioi.git.controller.GitRepo;
import it.uniroma2.alessandrolioi.git.exceptions.GitException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.Jira;
import it.uniroma2.alessandrolioi.jira.exceptions.JiraRESTException;
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
            bookkeeper.classifyIssues(bookkeeperVersions, bookkeeperIssues);
            double avroColdStart = calculateColdStart("avro");
            bookkeeper.applyProportionIncrement(bookkeeperVersions, avroColdStart);
            for (JiraIssue i : bookkeeperIssues) {
                String injected = i.getIvIndex() != -1 ? bookkeeperVersions.get(i.getIvIndex()).name() : "x.x.x";
                String opening = bookkeeperVersions.get(i.getOvIndex()).name();
                String fix = bookkeeperVersions.get(i.getFvIndex()).name();
                int fvIvDiff = i.getFvIndex() - i.getIvIndex();
                int fvOvDiff = Math.max(i.getFvIndex() - i.getOvIndex(), 1);
                System.out.printf("%s | IV: %s, OV: %s, FV: %s | FV-IV: %d, FV-OV: %d%n", i.getKey(), injected, opening, fix, fvIvDiff, fvOvDiff);
            }
        } catch (JiraRESTException e) {
            throw new RuntimeException(e);
        }
    }

    private static double calculateColdStart(String project) throws JiraRESTException {
        Jira avro = new Jira(project);
        List<JiraVersion> avroVersions = avro.loadVersions();
        List<JiraIssue> avroIssues = avro.loadIssues(avroVersions.get(0).releaseDate(), avroVersions.get(avroVersions.size() - 1).releaseDate());
        avro.classifyIssues(avroVersions, avroIssues);
        return avro.calculateProportionColdStart(avroIssues);
    }
}

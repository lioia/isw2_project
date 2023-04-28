package it.uniroma2.alessandrolioi;

import it.uniroma2.alessandrolioi.git.controller.GitRepo;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.exceptions.GitRepoException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.JiraGitIntegration;
import it.uniroma2.alessandrolioi.jira.Jira;
import it.uniroma2.alessandrolioi.jira.exceptions.JiraRESTException;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class App {
    public static void main(String[] args) {
        String project = "bookkeeper";
        String coldStartProject = "avro";
        GitRepo repo = null;
        Jira bookkeeper = new Jira(project);
        try {
            List<JiraVersion> bookkeeperVersions = bookkeeper.loadVersions();

            JiraVersion firstVersion = bookkeeperVersions.get(0);
            JiraVersion lastVersion = bookkeeperVersions.get(bookkeeperVersions.size() - 1);

            // Could be slow since it has to download ~90 MiB for BookKeeper and ~30 MiB for Avro
            repo = new GitRepo(project, "https://github.com/apache/%s".formatted(project), "master");
            List<GitCommitEntry> commits = repo.getCommits();

            List<JiraIssue> bookkeeperIssues = bookkeeper.loadIssues(firstVersion.releaseDate(), lastVersion.releaseDate());
            bookkeeper.classifyIssues(bookkeeperVersions, bookkeeperIssues);
            double avroColdStart = calculateColdStart(coldStartProject);
            bookkeeper.applyProportionIncrement(bookkeeperVersions, avroColdStart);

            JiraGitIntegration integration = new JiraGitIntegration(bookkeeperVersions, commits);
            for (Map.Entry<JiraVersion, GitCommitEntry> entry : integration.getRevisions().entrySet()) {
                System.out.printf("%s - %s%n", entry.getKey().name(), entry.getValue().hash());
            }
        } catch (JiraRESTException | GitRepoException | GitLogException e) {
            throw new RuntimeException(e);
        } finally {
            Objects.requireNonNull(repo).clean();
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

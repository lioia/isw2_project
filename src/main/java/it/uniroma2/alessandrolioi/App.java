package it.uniroma2.alessandrolioi;

import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.exceptions.GitRepoException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import it.uniroma2.alessandrolioi.integration.JiraGitIntegration;
import it.uniroma2.alessandrolioi.integration.exceptions.NotFoundException;
import it.uniroma2.alessandrolioi.jira.Jira;
import it.uniroma2.alessandrolioi.jira.exceptions.JiraRestException;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        String project = "bookkeeper";
        String coldStartProject = "avro";
        Git git = null;
        try {
            Jira bookkeeper = new Jira(project);
            Jira avro = new Jira(coldStartProject);
            double coldStart = avro.calculateColdStart();
            bookkeeper.applyProportion(coldStart);

            git = new Git(project, "https://github.com/apache/bookkeeper", "master");
//            git = new Git(project);

            JiraGitIntegration integration = new JiraGitIntegration(git.getCommits());
            Map<JiraVersion, GitCommitEntry> revisions = integration.findRevisionsOfVersions(bookkeeper.getVersions());
            for (JiraVersion version : bookkeeper.getVersions()) {
                List<String> classes = git.getClassList(revisions.get(version));
                Map<String, GitDiffEntry> diffs = git.getDifferences(
                        revisions.get(bookkeeper.getVersions().get(0)), // First release
                        revisions.get(bookkeeper.getVersions().get(1)) // Second release
                );
                for (String aClass : classes) {
                    GitDiffEntry diff = diffs.get(aClass);
                    if (diff == null) System.out.printf("No diff found for class %s%n%n", aClass);
                    else System.out.printf("%s: %d %d%n%n", aClass, diff.added(), diff.deleted());
                }
            }
        } catch (JiraRestException | GitRepoException | GitLogException | NotFoundException | GitDiffException e) {
            e.printStackTrace();
        } finally {
            if (git != null) git.close();
        }
    }
}

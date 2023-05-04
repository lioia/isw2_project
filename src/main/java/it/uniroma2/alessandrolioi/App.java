package it.uniroma2.alessandrolioi;

import it.uniroma2.alessandrolioi.dataset.DatasetBuilder;
import it.uniroma2.alessandrolioi.dataset.exceptions.DatasetWriterException;
import it.uniroma2.alessandrolioi.dataset.exceptions.MetricException;
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
            DatasetBuilder dataset = new DatasetBuilder(revisions, git);
            dataset.applyLOCMetric();
            dataset.writeToFile("output.csv");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (git != null) git.close();
        }
    }
}

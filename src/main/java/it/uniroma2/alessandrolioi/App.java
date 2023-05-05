package it.uniroma2.alessandrolioi;

import it.uniroma2.alessandrolioi.dataset.DatasetBuilder;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.JiraGitIntegration;
import it.uniroma2.alessandrolioi.jira.Jira;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    static Logger logger = Logger.getLogger(App.class.getName());

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

            JiraGitIntegration integration = new JiraGitIntegration(git.getCommits());
            Map<JiraVersion, GitCommitEntry> revisions = integration.findRevisionsOfVersions(bookkeeper.getVersions());
            DatasetBuilder dataset = new DatasetBuilder(revisions, git);
            dataset.applyLOCMetric();
            dataset.applyLOCTouchedMetric();
            dataset.applyChurnMetric();
            dataset.writeToFile("output.csv");
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
        } finally {
            if (git != null) {
                boolean cleaned = git.close();
                if (!cleaned)
                    logger.log(Level.SEVERE, "Could not clean git repository for %s".formatted(project));
            }
        }
    }
}

package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.dataset.DatasetBuilder;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.JiraGitIntegration;
import it.uniroma2.alessandrolioi.jira.Jira;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.logging.Logger;

public class CsvGenerator {
    private static final Logger logger = Logger.getLogger("CsvGenerator");

    public static void main(String[] args) {
        String project = "bookkeeper";
        String coldStartProject = "avro";
        Git git = null;
        try {
            Jira bookkeeper = new Jira(project);
            Jira avro = new Jira(coldStartProject);
            double coldStart = avro.calculateColdStart();
            bookkeeper.applyProportion(coldStart);

            git = new Git(project, "https://github.com/apache/%s".formatted(project), "master");

            logger.info("Loading integration between Jira and Git");
            JiraGitIntegration integration = new JiraGitIntegration(git.getCommits());
            integration.findRevisions(bookkeeper.getVersions());

            for (Pair<JiraVersion, GitCommitEntry> version : integration.versions())
                git.loadClassesOfRevision(version.second());

            logger.info("Creating dataset");
            DatasetBuilder dataset = new DatasetBuilder(integration, git);
            dataset.applyMetrics();
            for (int i = 1; i <= bookkeeper.getVersions().size(); i++) {
                String outputName = "%sDataset%d.csv".formatted(project, i);
                dataset.setBuggy(i);
                dataset.writeToFile(outputName, i);
                logger.info("Dataset successfully created (%s)".formatted(outputName));
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        } finally {
            if (git != null) {
                boolean cleaned = git.close();
                if (!cleaned)
                    logger.severe("Could not clean git repository for %s".formatted(project));
            }
        }
    }
}

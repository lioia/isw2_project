package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.common.Projects;
import it.uniroma2.alessandrolioi.dataset.DatasetBuilder;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.JiraGitIntegration;
import it.uniroma2.alessandrolioi.jira.Jira;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatasetGeneratorApp {
    private static final Logger logger = Logger.getLogger("DatasetGenerator");

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < Projects.names().length; i++) {
            String project = Projects.names()[i];
            String coldStartProject = Projects.names()[Projects.names().length - i - 1];

            projectGeneration(project, coldStartProject);
        }
    }

    private static void projectGeneration(String project, String coldStartProject) throws IOException {
        Git git = null;
        try {
            Jira jiraProject = new Jira(project);
            Jira jiraColdStartProject = new Jira(coldStartProject);
            double coldStart = jiraColdStartProject.calculateColdStart();
            jiraProject.applyProportion(coldStart);

            git = new Git(project, "https://github.com/apache/%s".formatted(project), "master");

            if (logger.isLoggable(Level.INFO))
                logger.info("Loading integration between Jira and Git");
            JiraGitIntegration integration = new JiraGitIntegration(git.getCommits());
            integration.findRevisions(jiraProject.getVersions());

            for (Pair<JiraVersion, GitCommitEntry> version : integration.versions())
                git.loadClassesOfRevision(version.second());

            if (logger.isLoggable(Level.INFO))
                logger.info("Creating dataset");
            DatasetBuilder dataset = new DatasetBuilder(integration, git);
            dataset.applyMetrics();
            dataset.setBuggy(jiraProject.getVersions().size());
            dataset.writeToFile(project, jiraProject.getVersions().size());
            if (logger.isLoggable(Level.INFO))
                logger.info("Dataset successfully created: %s with %d releases".formatted(project, jiraProject.getVersions().size()));
        } catch (Exception e) {
            if (logger.isLoggable(Level.SEVERE))
                logger.severe(e.getMessage());
        } finally {
            if (git != null && !git.close() && logger.isLoggable(Level.SEVERE))
                logger.severe("Could not clean git repository for %s".formatted(project));
        }
    }
}

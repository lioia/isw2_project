package it.uniroma2.alessandrolioi;

import it.uniroma2.alessandrolioi.dataset.DatasetBuilder;
import it.uniroma2.alessandrolioi.git.Git;
import it.uniroma2.alessandrolioi.integration.JiraGitIntegration;
import it.uniroma2.alessandrolioi.jira.Jira;

import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    static final Logger logger = Logger.getLogger("Main");

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

            logger.log(Level.INFO, "Loading integration between Jira and Git");
            JiraGitIntegration integration = new JiraGitIntegration(git.getCommits());
            integration.findRevisions(bookkeeper.getVersions());
            git.loadClassesOfRevisions(integration.revisions().values().stream().toList());
            logger.log(Level.INFO, "Loading integration between Jira and Git");
            logger.log(Level.INFO, "Creating dataset");
            DatasetBuilder dataset = new DatasetBuilder(integration, git);
            dataset.applyMetrics();
            for (int i = 1; i <= bookkeeper.getVersions().size(); i++) {
                String outputName = "%sDataset%d.csv".formatted(project, i);
                dataset.setBuggy(i);
                dataset.writeToFile(outputName, i);
                logger.log(Level.INFO, "Dataset successfully created (%s)".formatted(outputName));
            }
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

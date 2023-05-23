package it.uniroma2.alessandrolioi.jira;

import it.uniroma2.alessandrolioi.jira.controllers.JiraIssueController;
import it.uniroma2.alessandrolioi.jira.controllers.JiraProportionController;
import it.uniroma2.alessandrolioi.jira.controllers.JiraVersionController;
import it.uniroma2.alessandrolioi.jira.exceptions.JiraRestException;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Jira {
    static final Logger logger = Logger.getLogger("Jira");

    private final List<JiraVersion> versions;
    private final List<JiraIssue> issues;

    public Jira(String project) throws JiraRestException {
        if(logger.isLoggable(Level.INFO))
            logger.info("Loading versions and tickets");
        JiraVersionController versionController = new JiraVersionController();
        JiraIssueController issueController = new JiraIssueController();

        // Load versions from Jira API
        versions = versionController.loadVersions(project);
        JiraVersion first = versions.get(0);
        JiraVersion last = versions.get(versions.size() - 1);
        // Load issues from Jira API from the first and last version considered
        issues = issueController.loadIssues(project, first.releaseDate(), last.releaseDate());
        // Initial version classification
        issueController.classifyIssues(versions, issues);
        if(logger.isLoggable(Level.INFO))
            logger.info("Versions and tickets successfully loaded");
    }

    public double calculateColdStart() {
        JiraProportionController controller = new JiraProportionController();
        return controller.calculateProportionColdStart(issues);
    }

    public void applyProportion(double coldStart) {
        JiraProportionController controller = new JiraProportionController();
        controller.applyProportionIncrement(versions, coldStart);
    }

    public List<JiraVersion> getVersions() {
        return versions;
    }
}

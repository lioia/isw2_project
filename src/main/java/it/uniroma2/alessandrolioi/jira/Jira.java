package it.uniroma2.alessandrolioi.jira;

import it.uniroma2.alessandrolioi.jira.controllers.JiraIssueController;
import it.uniroma2.alessandrolioi.jira.controllers.JiraProportionController;
import it.uniroma2.alessandrolioi.jira.controllers.JiraVersionController;
import it.uniroma2.alessandrolioi.jira.exceptions.JiraRestException;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.List;

public class Jira {
    private final List<JiraVersion> versions;
    private final List<JiraIssue> issues;

    public Jira(String project) throws JiraRestException {
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

package it.uniroma2.alessandrolioi.jira.models;

public record JiraCompleteIssue(JiraIssue baseIssue, JiraVersion injectedVersion, JiraVersion openingVersion,
                                JiraVersion fixVersion) {
}

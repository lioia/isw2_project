package it.uniroma2.alessandrolioi.jira.models;

public class JiraCompleteIssue extends JiraIssue {
    private final JiraVersion injected;
    private final JiraVersion opening;
    private final JiraVersion fix;

    public JiraCompleteIssue(JiraIssue base, JiraVersion injected, JiraVersion opening, JiraVersion fix) {
        super(base.getKey(), base.getResolution(), base.getCreated(), base.getAffectedVersionsDates());
        this.injected = injected;
        this.opening = opening;
        this.fix = fix;
    }

    public JiraVersion getInjected() {
        return injected;
    }

    public JiraVersion getOpening() {
        return opening;
    }

    public JiraVersion getFix() {
        return fix;
    }
}

package it.uniroma2.alessandrolioi.jira.models;

import java.util.List;

public class JiraCompleteIssue extends JiraIssue {
    private JiraVersion injected;
    private final JiraVersion opening;
    private final JiraVersion fix;
    private final int fvOvDifference;
    private int fvIvDifference;

    public JiraCompleteIssue(JiraIssue base, JiraVersion injected, JiraVersion opening, JiraVersion fix, int fvOvDifference) {
        this(base, injected, opening, fix, fvOvDifference, 0);
    }

    public JiraCompleteIssue(JiraIssue base, JiraVersion injected, JiraVersion opening, JiraVersion fix, int fvOvDifference, int fvIvDifference) {
        super(base.getKey(), base.getResolution(), base.getCreated(), base.getAffectedVersionsDates());
        this.injected = injected;
        this.opening = opening;
        this.fix = fix;
        this.fvOvDifference = fvOvDifference;
        this.fvIvDifference = fvIvDifference;
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

    public int getFvOvDifference() {
        return fvOvDifference;
    }

    public int getFvIvDifference() {
        return fvIvDifference;
    }

    public void setInjected(JiraVersion injected, int fvIvDifference) {
        this.injected = injected;
        this.fvIvDifference = fvIvDifference;
    }
}

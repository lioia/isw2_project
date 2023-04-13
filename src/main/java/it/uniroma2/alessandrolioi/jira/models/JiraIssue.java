package it.uniroma2.alessandrolioi.jira.models;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class JiraIssue {
    private final String key;
    private final LocalDate resolution;
    private final LocalDate created;
    private final List<LocalDate> affectedVersionsDates;

    public JiraIssue(String key, LocalDate resolution, LocalDate created,
                     // corresponds to the `version` field
                     // sorted list, but it can be empty
                     List<LocalDate> affectedVersionsDates
    ) {
        this.key = key;
        this.resolution = resolution;
        this.created = created;
        this.affectedVersionsDates = affectedVersionsDates;
    }

    public String getKey() {
        return key;
    }

    public LocalDate getResolution() {
        return resolution;
    }

    public LocalDate getCreated() {
        return created;
    }

    public List<LocalDate> getAffectedVersionsDates() {
        return affectedVersionsDates;
    }
}

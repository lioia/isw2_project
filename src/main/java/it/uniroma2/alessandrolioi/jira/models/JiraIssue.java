package it.uniroma2.alessandrolioi.jira.models;

import java.time.LocalDate;
import java.util.List;

public record JiraIssue(String key, LocalDate resolution, LocalDate created,
                        // corresponds to the `version` field
                        // sorted list, but it can be empty
                        List<LocalDate> affectedVersionsDates
) {
}

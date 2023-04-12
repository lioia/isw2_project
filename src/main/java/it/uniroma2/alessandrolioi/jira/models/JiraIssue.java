package it.uniroma2.alessandrolioi.jira.models;

import java.time.LocalDate;
import java.util.List;

public record JiraIssue(String key, LocalDate resolution, LocalDate created,
                        List<LocalDate> affectedVersions, // corresponds to the `versions` field
                        List<LocalDate> fixVersions) {
}

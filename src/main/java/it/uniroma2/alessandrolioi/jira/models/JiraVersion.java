package it.uniroma2.alessandrolioi.jira.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record JiraVersion(String id, String name, LocalDate releaseDate,
                          List<JiraIssue> injected, List<JiraIssue> opened, List<JiraIssue> fixed) {
    public JiraVersion(String id, String name, LocalDate releaseDate) {
        this(id, name, releaseDate, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    // JSON named fields
    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String RELEASE_DATE_FIELD = "releaseDate";
}

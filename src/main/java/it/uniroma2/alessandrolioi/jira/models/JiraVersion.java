package it.uniroma2.alessandrolioi.jira.models;

import java.time.LocalDate;
import java.util.*;

public final class JiraVersion {
    // JSON named fields
    public static final String NAME_FIELD = "name";
    public static final String RELEASE_DATE_FIELD = "releaseDate";
    public static final String RELEASED_FIELD = "released";

    // Version name | i.e. 4.0.0
    private final String name;
    // Version Release Date
    private final LocalDate releaseDate;
    // List of injected issues in this release
    private final List<JiraIssue> injected;
    // List of opened issues in this release
    private final List<JiraIssue> opened;
    // List of fixed issues in this release
    private final List<JiraIssue> fixed;

    public JiraVersion(String name, LocalDate releaseDate) {
        this.name = name;
        this.releaseDate = releaseDate;
        this.injected = new ArrayList<>();
        this.opened = new ArrayList<>();
        this.fixed = new ArrayList<>();
    }

    public String name() {
        return name;
    }

    public LocalDate releaseDate() {
        return releaseDate;
    }

    public List<JiraIssue> injected() {
        return injected;
    }

    public List<JiraIssue> opened() {
        return opened;
    }

    public List<JiraIssue> fixed() {
        return fixed;
    }
}

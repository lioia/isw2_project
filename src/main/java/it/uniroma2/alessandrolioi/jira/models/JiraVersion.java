package it.uniroma2.alessandrolioi.jira.models;

import java.time.LocalDate;

public record JiraVersion(String id, String name, LocalDate releaseDate) {
}

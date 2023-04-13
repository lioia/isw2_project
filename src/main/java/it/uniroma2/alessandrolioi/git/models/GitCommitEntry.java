package it.uniroma2.alessandrolioi.git.models;

import java.time.LocalDate;

public record GitCommitEntry(String hash, String message, String authorName, String authorEmail, LocalDate commitDate) {
}

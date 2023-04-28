package it.uniroma2.alessandrolioi.git.models;

import org.eclipse.jgit.revwalk.RevTree;

import java.time.LocalDateTime;

public record GitCommitEntry(String hash, String message, String authorName, String authorEmail,
                             LocalDateTime commitDate, RevTree tree) {
}

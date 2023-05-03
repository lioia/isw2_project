package it.uniroma2.alessandrolioi.git.models;

import org.eclipse.jgit.diff.DiffEntry;

// TODO maybe save just the ChangeType instead of the all entry
public record GitDiffEntry(DiffEntry entry, int added, int deleted) {
}

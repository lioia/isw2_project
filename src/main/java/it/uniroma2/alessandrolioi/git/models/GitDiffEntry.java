package it.uniroma2.alessandrolioi.git.models;

import org.eclipse.jgit.diff.DiffEntry;

public record GitDiffEntry(DiffEntry entry, int added, int deleted) {
    public int touched() {
        return this.added + this.deleted;
    }

    public int churn() {
        return this.added - this.deleted;
    }
}

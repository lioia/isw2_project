package it.uniroma2.alessandrolioi.dataset.models;

import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;

import java.util.List;

public record RevisionPairInfo(GitCommitEntry first, GitCommitEntry second, List<GitCommitEntry> commits) {
}

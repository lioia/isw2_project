package it.uniroma2.alessandrolioi.git.models;

import org.eclipse.jgit.revwalk.RevTree;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class GitCommitEntry {
    private final String hash;
    private final String message;
    private final LocalDateTime commitDate;
    private List<String> classList;
    private final RevTree tree;

    public GitCommitEntry(String hash, String message, LocalDateTime commitDate, RevTree tree) {
        this.hash = hash;
        this.message = message;
        this.commitDate = commitDate;
        this.tree = tree;

        this.classList = new ArrayList<>();
    }

    public String hash() {
        return hash;
    }

    public String message() {
        return message;
    }

    public LocalDateTime commitDate() {
        return commitDate;
    }

    public List<String> classList() {
        return classList;
    }

    public void setClassList(List<String> classList) {
        this.classList = classList;
    }

    public RevTree tree() {
        return tree;
    }
}

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
    private String author;
    private final RevTree tree;
    private final List<RevTree> parents;

    public GitCommitEntry(String hash, String message, LocalDateTime commitDate, String author, RevTree tree, List<RevTree> parents) {
        this.hash = hash;
        this.message = message;
        this.commitDate = commitDate;
        this.author = author;
        this.tree = tree;
        this.parents = parents;

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

    public String author() {
        return author;
    }

    public RevTree tree() {
        return tree;
    }

    public List<RevTree> parents() {
        return parents;
    }
}

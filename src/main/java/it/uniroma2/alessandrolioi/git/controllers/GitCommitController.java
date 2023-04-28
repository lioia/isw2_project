package it.uniroma2.alessandrolioi.git.controllers;

import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitCommitController {
    private final Repository repository;

    public GitCommitController(Repository repository) {
        this.repository = repository;
    }

    public List<String> getClassList(GitCommitEntry commit) throws GitLogException {
        try (TreeWalk walk = new TreeWalk(repository)) {
            List<String> classes = new ArrayList<>();
            // Set base commit
            walk.addTree(commit.tree());
            // Explore sub-folders
            walk.setRecursive(true);
            // Exclude non-java files
            walk.setFilter(PathSuffixFilter.create(".java"));

            // Iterate until there are files
            while (walk.next()) classes.add(walk.getPathString());

            return classes;
        } catch (IOException e) {
            throw new GitLogException("IO failure. Could not access refs", e);
        }
    }

    public List<String> getDifferences(GitCommitEntry first, GitCommitEntry second) {
        return new ArrayList<>();
    }
}

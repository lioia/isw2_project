package it.uniroma2.alessandrolioi.git.controllers;

import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Map<String, GitDiffEntry> getDifferences(GitCommitEntry first, GitCommitEntry second) throws GitDiffException {
        // Create a formatter disabling output
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            // Set current repository
            diffFormatter.setRepository(repository);
            // Exclude non-java files
            diffFormatter.setPathFilter(PathSuffixFilter.create(".java"));
            // Get diffs between `first` and `second` commits
            List<DiffEntry> diffs = diffFormatter.scan(first.tree(), second.tree());
            // List of computed differences
            Map<String, GitDiffEntry> differences = new HashMap<>();
            for (DiffEntry diff : diffs) {
                FileHeader header = diffFormatter.toFileHeader(diff);
                // Calculate added and deleted lines based on the edit list
                int added = 0;
                int deleted = 0;
                for (Edit edit : header.toEditList()) {
                    switch (edit.getType()) {
                        case INSERT -> added += edit.getLengthB() - edit.getLengthA();
                        case DELETE -> deleted += edit.getLengthA() - edit.getLengthB();
                        case REPLACE -> {
                            int replace = edit.getLengthB() - edit.getLengthA();
                            if (replace > 0) added += replace;
                            else if (replace < 0) deleted += replace;
                        }
                    }
                }
                String path = diff.getNewPath();
                GitDiffEntry entry = new GitDiffEntry(diff, added, deleted);
                differences.put(path, entry);
            }
            return differences;
        } catch (CorruptObjectException e) {
            throw new GitDiffException("Corrupt entry", e);
        } catch (MissingObjectException e) {
            throw new GitDiffException("Missing entry", e);
        } catch (IOException e) {
            throw new GitDiffException("Could not load commits", e);
        }
    }
}

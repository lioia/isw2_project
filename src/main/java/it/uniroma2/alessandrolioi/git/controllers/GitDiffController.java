package it.uniroma2.alessandrolioi.git.controllers;

import it.uniroma2.alessandrolioi.common.Pair;
import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitDiffController {
    public List<GitDiffEntry> getAllDifferencesOfClass(Repository repository, List<GitCommitEntry> commitsInBetween, String path) throws GitDiffException {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setPathFilter(PathFilter.create(path));
            List<GitDiffEntry> diffEntries = new ArrayList<>();
            if (commitsInBetween.isEmpty()) return diffEntries;
            GitCommitEntry previous = commitsInBetween.get(0);
            for (int i = 1; i < commitsInBetween.size(); i++) {
                GitCommitEntry current = commitsInBetween.get(i);
                List<DiffEntry> diffs = diffFormatter.scan(previous.tree(), current.tree());
                for (DiffEntry diff : diffs) {
                    FileHeader header = diffFormatter.toFileHeader(diff);
                    Pair<Integer, Integer> addedAndDeleted = calculateAddedAndDeleted(header.toEditList());
                    GitDiffEntry entry = new GitDiffEntry(diff, addedAndDeleted.first(), addedAndDeleted.second());
                    diffEntries.add(entry);
                }
            }
            return diffEntries;
        } catch (CorruptObjectException e) {
            throw new GitDiffException("Corrupt entry", e);
        } catch (MissingObjectException e) {
            throw new GitDiffException("Missing Entry", e);
        } catch (IOException e) {
            throw new GitDiffException("Could not load commits", e);
        }
    }

    public Map<String, GitDiffEntry> getDifferences(Repository repository, GitCommitEntry first, GitCommitEntry second) throws GitDiffException {
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
                Pair<Integer, Integer> addedAndDeleted = calculateAddedAndDeleted(header.toEditList());
                String path = diff.getNewPath();
                GitDiffEntry entry = new GitDiffEntry(diff, addedAndDeleted.first(), addedAndDeleted.second());
                differences.put(path, entry);
            }
            return differences;
        } catch (CorruptObjectException e) {
            throw new GitDiffException("Corrupt entry", e);
        } catch (MissingObjectException e) {
            throw new GitDiffException("Missing entry", e);
        } catch (IOException e) {
            throw new GitDiffException("Could not load commit", e);
        }
    }

    public List<String> getModifiedClassesOfCommit(Repository repository, GitCommitEntry commit) throws GitDiffException {
        try (Git git = new Git(repository)) {
            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser tree = new CanonicalTreeParser();
            tree.reset(reader, commit.tree());
            DiffCommand command = git.diff().setNewTree(tree);
            List<String> modified = new ArrayList<>();
            for (RevTree parentTree : commit.parents()) {
                CanonicalTreeParser parent = new CanonicalTreeParser();
                parent.reset(reader, parentTree);
                command.setOldTree(parent);
                List<String> classes = command.call().stream().map(DiffEntry::getNewPath).toList();
                modified.addAll(classes);
            }
            return modified;
        } catch (IOException e) {
            throw new GitDiffException("Tree is invalid", e);
        } catch (GitAPIException e) {
            throw new GitDiffException("Could not execute diff command", e);
        }
    }

    // Calculate added and deleted lines based on the edit list
    private Pair<Integer, Integer> calculateAddedAndDeleted(EditList list) {
        int added = 0;
        int deleted = 0;
        for (Edit edit : list) {
            int lengthDifference = edit.getLengthB() - edit.getLengthA();
            if (edit.getType() == Edit.Type.INSERT)
                added += lengthDifference;
            else if (edit.getType() == Edit.Type.DELETE)
                deleted -= lengthDifference;
            else if (edit.getType() == Edit.Type.REPLACE) {
                if (lengthDifference > 0) added += lengthDifference;
                else if (lengthDifference < 0) deleted += lengthDifference;
            }
        }
        return new Pair<>(added, deleted);
    }
}

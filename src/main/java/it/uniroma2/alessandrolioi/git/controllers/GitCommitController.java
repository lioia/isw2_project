package it.uniroma2.alessandrolioi.git.controllers;

import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.exceptions.GitFileException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import it.uniroma2.alessandrolioi.utils.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class GitCommitController {
    public List<GitCommitEntry> getCommits(Repository repository) throws GitLogException {
        List<GitCommitEntry> entries = new ArrayList<>();
        try (Git git = new Git(repository)) {
            for (Ref branch : git.branchList().call()) {
                // all: used to get the commits from all branches (even the branches not synced with GitHub, but only in SVN ~ pre-2017)
                for (RevCommit commit : git.log().all().add(repository.resolve(branch.getName())).call()) {
                    String hash = commit.getName();
                    String message = commit.getShortMessage();
                    String authorName = commit.getAuthorIdent().getName();
                    String authorEmail = commit.getAuthorIdent().getEmailAddress();
                    LocalDateTime date = LocalDateTime.ofInstant(commit.getCommitterIdent().getWhenAsInstant(), commit.getCommitterIdent().getZoneId());
                    RevTree tree = commit.getTree();
                    GitCommitEntry entry = new GitCommitEntry(hash, message, authorName, authorEmail, date, tree);
                    entries.add(entry);
                }
            }
        } catch (GitAPIException e) {
            throw new GitLogException("Unable to get the log", e);
        } catch (AmbiguousObjectException | IncorrectObjectTypeException e) {
            throw new GitLogException("Not a commit", e);
        } catch (IOException e) {
            throw new GitLogException("IO failure. Could not access refs", e);
        }

        // Ascending order of commit date
        Collections.reverse(entries);
        return entries;
    }

    public List<String> getClassList(Repository repository, GitCommitEntry commit) throws GitLogException {
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

    public String getContentsOfFile(Repository repository, GitCommitEntry commit, String fileName) throws GitFileException {
        try (TreeWalk walk = TreeWalk.forPath(repository, fileName, commit.tree())) {
            ObjectId blobId = walk.getObjectId(0);
            ObjectReader reader = repository.newObjectReader();
            ObjectLoader loader = reader.open(blobId);
            return new String(loader.getBytes(), StandardCharsets.UTF_8);
        } catch (CorruptObjectException e) {
            throw new GitFileException("Corrupt git object", e);
        } catch (IncorrectObjectTypeException e) {
            throw new GitFileException("Unexpected git object type", e);
        } catch (MissingObjectException e) {
            throw new GitFileException("Git object not found", e);
        } catch (IOException e) {
            throw new GitFileException("Reading failed", e);
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
            throw new GitDiffException("Could not load commits", e);
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

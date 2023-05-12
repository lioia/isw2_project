package it.uniroma2.alessandrolioi.git.controllers;

import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.exceptions.GitFileException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import it.uniroma2.alessandrolioi.common.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
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
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class GitCommitController {
    private static final String LOAD_EXCEPTION_MESSAGE = "Could not load commits";
    private static final String MISSING_EXCEPTION_MESSAGE = "Missing entry";

    public List<GitCommitEntry> getCommits(Repository repository) throws GitLogException {
        List<GitCommitEntry> entries = new ArrayList<>();
        try (Git git = new Git(repository)) {
            for (Ref branch : git.branchList().call()) {
                // all: used to get the commits from all branches (even the branches not synced with GitHub, but only in SVN ~ pre-2017)
                for (RevCommit commit : git.log().all().add(repository.resolve(branch.getName())).call())
                    entries.add(commitFromRevCommit(commit));
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

    public List<String> getClassList(Repository repository, RevTree tree) throws GitLogException {
        try (TreeWalk walk = new TreeWalk(repository)) {
            List<String> classes = new ArrayList<>();
            // Set base commit
            walk.addTree(tree);
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

    public List<GitCommitEntry> getAllCommitsOfClass(Repository repository, GitCommitEntry first, GitCommitEntry second, String path) throws GitLogException {
        try (Git git = new Git(repository)) {
            ObjectId firstId = ObjectId.fromString(first.hash());
            ObjectId secondId = ObjectId.fromString(second.hash());
            List<GitCommitEntry> entries = new ArrayList<>();
            git.log().addRange(firstId, secondId).addPath(path).call().iterator().forEachRemaining(c -> entries.add(commitFromRevCommit(c)));
            return entries;
        } catch (MissingObjectException e) {
            throw new GitLogException(MISSING_EXCEPTION_MESSAGE, e);
        } catch (IOException e) {
            throw new GitLogException(LOAD_EXCEPTION_MESSAGE, e);
        } catch (NoHeadException e) {
            throw new GitLogException("Could not find HEAD", e);
        } catch (GitAPIException e) {
            throw new GitLogException("Could not call git API", e);
        }
    }

    public List<GitDiffEntry> getAllDifferencesOfClass(Repository repository, GitCommitEntry first, GitCommitEntry second, String path) throws GitDiffException, GitLogException {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setPathFilter(PathFilter.create(path));
            List<GitDiffEntry> diffEntries = new ArrayList<>();
            List<GitCommitEntry> commitsInBetween = getAllCommitsOfClass(repository, first, second, path);
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
            throw new GitDiffException(MISSING_EXCEPTION_MESSAGE, e);
        } catch (IOException e) {
            throw new GitDiffException(LOAD_EXCEPTION_MESSAGE, e);
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
            throw new GitDiffException(MISSING_EXCEPTION_MESSAGE, e);
        } catch (IOException e) {
            throw new GitDiffException(LOAD_EXCEPTION_MESSAGE, e);
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

    private GitCommitEntry commitFromRevCommit(RevCommit commit) {
        String hash = commit.getName();
        String message = commit.getShortMessage();
        LocalDateTime date = LocalDateTime.ofInstant(commit.getCommitterIdent().getWhenAsInstant(), commit.getCommitterIdent().getZoneId());
        RevTree tree = commit.getTree();
        return new GitCommitEntry(hash, message, date, tree);
    }
}

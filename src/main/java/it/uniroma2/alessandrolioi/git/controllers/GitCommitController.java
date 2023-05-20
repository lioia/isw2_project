package it.uniroma2.alessandrolioi.git.controllers;

import it.uniroma2.alessandrolioi.git.exceptions.GitFileException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class GitCommitController {
    public GitCommitEntry getFirstCommit(Repository repository) throws GitLogException {
        try {
            RevWalk walk = new RevWalk(repository);
            ObjectId head = repository.resolve(Constants.HEAD);
            RevCommit root = walk.parseCommit(head);
            walk.sort(RevSort.REVERSE);
            walk.markStart(root);
            return commitFromRevCommit(walk.next());
        } catch (AmbiguousObjectException | IncorrectObjectTypeException e) {
            throw new GitLogException("Not a commit", e);
        } catch (IOException e) {
            throw new GitLogException(GitLogException.IO, e);
        }
    }

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
            throw new GitLogException(GitLogException.IO, e);
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
            throw new GitLogException(GitLogException.IO, e);
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
            throw new GitLogException("Missing entry", e);
        } catch (IOException e) {
            throw new GitLogException(GitLogException.IO, e);
        } catch (NoHeadException e) {
            throw new GitLogException("Could not find HEAD", e);
        } catch (GitAPIException e) {
            throw new GitLogException("Could not call git API", e);
        }
    }

    private GitCommitEntry commitFromRevCommit(RevCommit commit) {
        String hash = commit.getName();
        String message = commit.getShortMessage();
        LocalDateTime date = LocalDateTime.ofInstant(commit.getCommitterIdent().getWhenAsInstant(), commit.getCommitterIdent().getZoneId());
        RevTree tree = commit.getTree();
        List<RevTree> parents = Arrays.stream(commit.getParents()).map(RevCommit::getTree).toList();
        return new GitCommitEntry(hash, message, date, tree, parents);
    }
}

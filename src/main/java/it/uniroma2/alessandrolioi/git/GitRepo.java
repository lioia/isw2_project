package it.uniroma2.alessandrolioi.git;

import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.exceptions.GitRepoException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitRepo {
    private final String folderPath;
    private Repository repository;

    public GitRepo(String folderPath) throws GitRepoException {
        load(folderPath);
        this.folderPath = folderPath;
    }

    public GitRepo(String folderPath, String url, String branch) throws GitRepoException {
        download(folderPath, url, branch);
        load(folderPath);
        this.folderPath = folderPath;
    }

    public GitRepo(String folderPath, String url) throws GitRepoException {
        this(folderPath, url, "main");
    }

    public List<GitCommitEntry> getCommits() throws GitLogException {
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
                    GitCommitEntry entry = new GitCommitEntry(hash, message, authorName, authorEmail, date);
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

        Collections.reverse(entries); // Ascending order of commit date
        return entries;
    }

    private void load(String folderPath) throws GitRepoException {
        File folder = new File(folderPath);
        if (!folder.exists()) throw new GitRepoException("Local folder does not exists");
        if (folder.isFile()) throw new GitRepoException("The path points to a file");
        try {
            Path gitPath = folder.toPath().resolve(".git");
            repository = new RepositoryBuilder().setGitDir(gitPath.toFile()).build();
        } catch (IOException e) {
            throw new GitRepoException("Could not load repository", e);
        }
    }

    private void download(String folderPath, String url, String branch) throws GitRepoException {
        File folder = new File(folderPath);
        if (folder.exists()) throw new GitRepoException("Local folder already exists");
        try {
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(folder)
                    .setBranch(branch)
                    .call()
                    .close();
        } catch (GitAPIException e) {
            throw new GitRepoException("Could not clone repository", e);
        }
    }

    public boolean clean() {
        repository.close();
        return recurseClean(new File(folderPath));
    }

    private boolean recurseClean(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                // Early return (some files could not be deleted)
                if (!recurseClean(file)) return false;
            }
        }
        return folder.delete();
    }
}

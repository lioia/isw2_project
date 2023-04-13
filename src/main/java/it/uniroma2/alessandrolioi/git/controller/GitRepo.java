package it.uniroma2.alessandrolioi.git.controller;

import it.uniroma2.alessandrolioi.git.exceptions.GitException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GitRepo {
    private final String folderPath;
    private Repository repository;

    public GitRepo(String folderPath) throws GitException {
        load(folderPath);
        this.folderPath = folderPath;
    }

    public GitRepo(String folderPath, String url, String branch) throws GitException {
        download(folderPath, url, branch);
        load(folderPath);
        this.folderPath = folderPath;
    }

    public GitRepo(String folderPath, String url) throws GitException {
        this(folderPath, url, "main");
    }

    public List<GitCommitEntry> getCommits(LocalDate afterFilter, LocalDate beforeFilter) throws GitException {
        List<GitCommitEntry> entries = new ArrayList<>();
        try (Git git = new Git(repository)) {
            for (RevCommit commit : git.log().call()) {
                String hash = commit.getName();
                String message = commit.getFullMessage();
                String authorName = commit.getAuthorIdent().getName();
                String authorEmail = commit.getAuthorIdent().getEmailAddress();
                LocalDate date = LocalDate.ofInstant(commit.getCommitterIdent().getWhenAsInstant(), commit.getCommitterIdent().getZoneId());
                if (date.isBefore(afterFilter) || date.isAfter(beforeFilter)) continue;
                GitCommitEntry entry = new GitCommitEntry(hash, message, authorName, authorEmail, date);
                entries.add(entry);
            }
        } catch (GitAPIException e) {
            throw new GitException("LOG", "Unable to get the log", e);
        }
        return entries;
    }

    public List<GitCommitEntry> getCommits() throws GitException {
        return getCommits(LocalDate.MIN, LocalDate.MAX);
    }

    private void load(String folderPath) throws GitException {
        File folder = new File(folderPath);
        if (!folder.exists()) throw new GitException("LOCAL-CHECK", "Local folder does not exists");
        if (folder.isFile()) throw new GitException("LOCAL-CHECK", "The path points to a file");
        try {
            Path gitPath = folder.toPath().resolve(".git");
            repository = new RepositoryBuilder().setGitDir(gitPath.toFile()).build();
        } catch (IOException e) {
            throw new GitException("LOCAL-REPO", e);
        }
    }

    private void download(String folderPath, String url, String branch) throws GitException {
        File folder = new File(folderPath);
        if (folder.exists()) throw new GitException("REMOTE-CHECK", "Local folder already exists");
        try {
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(folder)
                    .setBranch(branch)
                    .call()
                    .close();
        } catch (GitAPIException e) {
            throw new GitException("REMOTE-CLONE", e);
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

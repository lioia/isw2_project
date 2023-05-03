package it.uniroma2.alessandrolioi.git.controllers;

import it.uniroma2.alessandrolioi.git.exceptions.GitRepoException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class GitRepoController {
    public Repository loadLocal(File folder) throws GitRepoException {
        if (!folder.exists()) throw new GitRepoException("Local folder does not exists");
        if (folder.isFile()) throw new GitRepoException("The path points to a file");
        try {
            Path gitPath = folder.toPath().resolve(".git");
            return new RepositoryBuilder().setGitDir(gitPath.toFile()).build();
        } catch (IOException e) {
            throw new GitRepoException("Could not load repository", e);
        }
    }

    public Repository cloneRemote(File folder, String url, String branch) throws GitRepoException {
        if (folder.exists()) throw new GitRepoException("Local folder already exists");
        try {
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(folder)
                    .setBranch(branch)
                    .call()
                    .close();
            return loadLocal(folder);
        } catch (GitAPIException e) {
            throw new GitRepoException("Could not clone repository", e);
        }
    }

    public boolean recurseClean(File folder) {
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

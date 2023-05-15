package it.uniroma2.alessandrolioi.git;

import it.uniroma2.alessandrolioi.git.controllers.GitCommitController;
import it.uniroma2.alessandrolioi.git.controllers.GitRepoController;
import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.exceptions.GitFileException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.exceptions.GitRepoException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Git {
    static final Logger logger = Logger.getLogger("Git");

    private final File folder;
    private final Repository repository;
    private final List<GitCommitEntry> commits;

    // Remote Repository
    public Git(String project, String url, String branch) throws GitRepoException, GitLogException {
        logger.info("Cloning remote repository (this might take a while)...");
        GitRepoController repoController = new GitRepoController();
        GitCommitController commitController = new GitCommitController();

        this.folder = new File(project);
        this.repository = repoController.cloneRemote(folder, url, branch);
        this.commits = commitController.getCommits(repository);
        logger.info("Repository successfully cloned");
    }

    // Local Repository
    public Git(String folderPath) throws GitRepoException, GitLogException {
        logger.info("Loading local repository");
        GitRepoController repoController = new GitRepoController();
        GitCommitController commitController = new GitCommitController();

        this.folder = new File(folderPath);
        this.repository = repoController.loadLocal(folder);
        this.commits = commitController.getCommits(repository);
        logger.info("Repository successfully loaded");
    }

    public boolean close() {
        GitRepoController controller = new GitRepoController();

        repository.close();
        return controller.recurseClean(folder);
    }

    public List<GitCommitEntry> getAllCommitsOfClass(GitCommitEntry first, GitCommitEntry second, String aClass) throws GitLogException {
        GitCommitController controller = new GitCommitController();
        return controller.getAllCommitsOfClass(repository, first, second, aClass);
    }

    public Map<String, GitDiffEntry> getDifferences(GitCommitEntry first, GitCommitEntry second) throws GitDiffException {
        GitCommitController controller = new GitCommitController();
        return controller.getDifferences(repository, first, second);
    }

    public List<GitDiffEntry> getAllDifferencesOfClass(GitCommitEntry first, GitCommitEntry second, String aClass) throws GitDiffException, GitLogException {
        GitCommitController controller = new GitCommitController();
        return controller.getAllDifferencesOfClass(repository, first, second, aClass);
    }

    public String getContentsOfClass(GitCommitEntry commit, String filePath) throws GitFileException {
        GitCommitController controller = new GitCommitController();
        return controller.getContentsOfFile(repository, commit, filePath);
    }

    public void loadClassesOfRevisions(List<GitCommitEntry> values) throws GitLogException {
        GitCommitController controller = new GitCommitController();
        for (GitCommitEntry commit : values)
            commit.setClassList(controller.getClassList(repository, commit.tree()));
    }

    public List<GitCommitEntry> getCommits() {
        return commits;
    }
}

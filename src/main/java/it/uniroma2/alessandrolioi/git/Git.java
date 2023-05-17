package it.uniroma2.alessandrolioi.git;

import it.uniroma2.alessandrolioi.git.controllers.GitCommitController;
import it.uniroma2.alessandrolioi.git.controllers.GitDiffController;
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
        GitDiffController controller = new GitDiffController();
        return controller.getDifferences(repository, first, second);
    }

    public List<GitDiffEntry> getAllDifferencesOfClass(GitCommitEntry first, GitCommitEntry second, String aClass) throws GitDiffException, GitLogException {
        GitCommitController commitController = new GitCommitController();
        GitDiffController diffController = new GitDiffController();
        List<GitCommitEntry> commitsInBetween = commitController.getAllCommitsOfClass(repository, first, second, aClass);
        return diffController.getAllDifferencesOfClass(repository, commitsInBetween, aClass);
    }

    public String getContentsOfClass(GitCommitEntry commit, String filePath) throws GitFileException {
        GitCommitController controller = new GitCommitController();
        return controller.getContentsOfFile(repository, commit, filePath);
    }

    public void loadClassesOfRevision(GitCommitEntry version) throws GitLogException {
        GitCommitController controller = new GitCommitController();
        version.setClassList(controller.getClassList(repository, version.tree()));
    }

    public List<String> getModifiedClassesOfCommit(GitCommitEntry commit) throws GitDiffException {
        GitDiffController controller = new GitDiffController();
        return controller.getModifiedClassesOfCommit(repository, commit);
    }

    public List<GitCommitEntry> getCommits() {
        return commits;
    }
}

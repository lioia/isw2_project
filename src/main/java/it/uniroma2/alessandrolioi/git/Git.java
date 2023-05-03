package it.uniroma2.alessandrolioi.git;

import it.uniroma2.alessandrolioi.git.controllers.GitCommitController;
import it.uniroma2.alessandrolioi.git.controllers.GitRepoController;
import it.uniroma2.alessandrolioi.git.exceptions.GitDiffException;
import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;
import it.uniroma2.alessandrolioi.git.exceptions.GitRepoException;
import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.git.models.GitDiffEntry;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Git {
    private final File folder;
    private final Repository repository;
    private final List<GitCommitEntry> commits;

    // Remote Repository
    public Git(String project, String url, String branch) throws GitRepoException, GitLogException {
        GitRepoController repoController = new GitRepoController();
        GitCommitController commitController = new GitCommitController();

        this.folder = new File(project);
        this.repository = repoController.cloneRemote(folder, url, branch);
        this.commits = commitController.getCommits(repository);
    }

    // Local Repository
    public Git(String folderPath) throws GitRepoException, GitLogException {
        GitRepoController repoController = new GitRepoController();
        GitCommitController commitController = new GitCommitController();

        this.folder = new File(folderPath);
        this.repository = repoController.loadLocal(folder);
        this.commits = commitController.getCommits(repository);
    }

    public boolean close() {
        GitRepoController controller = new GitRepoController();

        repository.close();
        return controller.recurseClean(folder);
    }

    public List<String> getClassList(GitCommitEntry commit) throws GitLogException {
        GitCommitController controller = new GitCommitController();
        return controller.getClassList(repository, commit);
    }

    public Map<String, GitDiffEntry> getDifferences(GitCommitEntry first, GitCommitEntry second) throws GitDiffException {
        GitCommitController controller = new GitCommitController();
        return controller.getDifferences(repository, first, second);
    }

    public List<GitCommitEntry> getCommits() {
        return commits;
    }
}

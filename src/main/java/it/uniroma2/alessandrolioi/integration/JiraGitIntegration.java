package it.uniroma2.alessandrolioi.integration;

import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.exceptions.NotFoundException;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

public class JiraGitIntegration {
    public Map<JiraVersion, GitCommitEntry> findRevisions(List<JiraVersion> versions, List<GitCommitEntry> commits) throws NotFoundException {
        Map<JiraVersion, GitCommitEntry> revisions = new HashMap<>();
        for (JiraVersion version : versions) {
            Optional<GitCommitEntry> candidate = useSemanticFilter(version.name(), commits);
            if (candidate.isEmpty()) candidate = useDateFilter(version.releaseDate(), commits);
            if (candidate.isEmpty()) throw new NotFoundException(version.name());
            revisions.put(version, candidate.get());
        }
        return revisions;
    }

    // Getting commits containing the version name in the commit message
    // (most commit releases follow the pattern `BookKeeper VERSION_NAME release` or `Tag* VERSION_NAME[ release]`)
    private Optional<GitCommitEntry> useSemanticFilter(String name, List<GitCommitEntry> commits) {
        Pattern avroPattern = Pattern.compile("Tag.* %s(| release)".formatted(name));
        List<GitCommitEntry> semanticFilter = commits.stream()
                .filter(c -> avroPattern.matcher(c.message()).find()
                        || c.message().contains("BookKeeper %s release".formatted(name)))
                .toList();
        if (semanticFilter.isEmpty()) return Optional.empty();
        return Optional.of(semanticFilter.get(semanticFilter.size() - 1));
    }

    // Returns the first commit after the version release date (end of day)
    private Optional<GitCommitEntry> useDateFilter(LocalDate releaseDate, List<GitCommitEntry> commits) {
        return commits.stream()
                .filter(c -> !c.commitDate().isBefore(releaseDate.atTime(LocalTime.MAX)))
                .findFirst();
    }
}

package it.uniroma2.alessandrolioi.integration;

import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class JiraGitIntegration {
    private final HashMap<JiraVersion, GitCommitEntry> revisions;

    public JiraGitIntegration(List<JiraVersion> versions, List<GitCommitEntry> commits) {
        revisions = new HashMap<>();
        for (JiraVersion version : versions) {
            Pattern avroPattern = Pattern.compile("Tag.* %s(| release)".formatted(version.name()));
            // Getting commits containing the version name in the commit message
            // (most commit releases follow the pattern `BookKeeper VERSION_NAME release` or `Tag* VERSION_NAME[ release]`)
            List<GitCommitEntry> semanticFilter = commits.stream()
                    .filter(c -> avroPattern.matcher(c.message()).find()
                            || c.message().contains("BookKeeper %s release".formatted(version.name())))
                    .toList();
            if (semanticFilter.size() != 0) {
                revisions.put(version, semanticFilter.get(semanticFilter.size() - 1));
                continue;
            }
            // If a version does not follow the pattern, the revision is the first commit after the version release date (end of day)
            List<GitCommitEntry> dateFilter = commits.stream().filter(c -> !c.commitDate().isBefore(version.releaseDate().atTime(LocalTime.MAX))).toList();
            if (dateFilter.size() != 0) {
                revisions.put(version, dateFilter.get(0));
                continue;
            }
            revisions.put(version, null);
        }
    }

    public HashMap<JiraVersion, GitCommitEntry> getRevisions() {
        return revisions;
    }
}

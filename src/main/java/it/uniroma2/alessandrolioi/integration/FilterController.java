package it.uniroma2.alessandrolioi.integration;

import it.uniroma2.alessandrolioi.git.models.GitCommitEntry;
import it.uniroma2.alessandrolioi.integration.exceptions.NotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

public class FilterController {
    // Getting commits containing the version name in the commit message
    // (most commit releases follow the pattern `BookKeeper VERSION_NAME release` or `Tag* VERSION_NAME[ release]`)
    public GitCommitEntry useSemanticFilter(String name, List<GitCommitEntry> commits) throws NotFoundException {
        Pattern avroPattern = Pattern.compile("Tag.* %s(| release)".formatted(name));
        List<GitCommitEntry> semanticFilter = commits.stream()
                .filter(c -> avroPattern.matcher(c.message()).find()
                        || c.message().contains("BookKeeper %s release".formatted(name)))
                .toList();
        if (semanticFilter.isEmpty()) throw new NotFoundException("Semantic filter failed for %s".formatted(name));
        return semanticFilter.get(semanticFilter.size() - 1);
    }

    // Returns the first commit after the version release date (end of day)
    public GitCommitEntry useDateFilter(LocalDate releaseDate, List<GitCommitEntry> commits) throws NotFoundException {
        return commits.stream()
                .filter(c -> !c.commitDate().isBefore(releaseDate.atTime(LocalTime.MAX)))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Date filter failed"));
    }

    // Returns the first commits starting with `key`
    public GitCommitEntry useSemanticKeyFilter(String key, List<GitCommitEntry> commits) throws NotFoundException {
        return commits.stream()
                .filter(c -> c.message().startsWith(key))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Semantic key filter failed for %s".formatted(key)));
    }
}

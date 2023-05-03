package it.uniroma2.alessandrolioi.jira.controllers;

import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;

import java.util.*;

public class JiraProportionController {
    public double calculateProportionColdStart(List<JiraIssue> issues) {
        // Get issues with valid IV (OV and FV should always be present)
        List<JiraIssue> filter = issues.stream().filter(i -> i.getIvIndex() != -1).toList();
        List<Double> proportions = filter.stream().map(JiraIssue::calculateProportion).toList();
        double sum = 0f;
        for (double value : proportions) sum += value;
        return sum / proportions.size();
    }

    public void applyProportionIncrement(List<JiraVersion> versions, double proportionColdStart) {
        // Sum from release 1 to R-1
        double lastSum = 0f;
        // Issues from release 1 to R-1
        int totalIssues = 0;
        // For each version R
        for (JiraVersion version : versions) {
            // Issues used to calculate proportion
            List<JiraIssue> valid = version.fixed().stream().filter(i -> i.getIvIndex() != -1).toList();
            double proportion = proportionColdStart; // use coldStart if there are less than 5 issues
            if (valid.size() >= 5) {
                List<Double> proportions = valid.stream().map(JiraIssue::calculateProportion).toList();
                double currentSum = proportions.stream().reduce(0.0, Double::sum);
                /*
                 * Incremental Proportion
                 *   lastSum: sum of proportions from release 1 to R-1
                 *   (lastProportion * totalIssues) + currentSum: sum of proportions from release 1 to R
                 *   ((lastProportion * totalIssues) + currentSum) / (totalIssues + valid.size): proportion mean
                 * */
                proportion = (lastSum + currentSum) / (totalIssues + valid.size());
                lastSum += currentSum;
            }
            // Get issue opened in this release without IV
            List<JiraIssue> invalid = new ArrayList<>(version.fixed());
            invalid.removeAll(valid);
            for (JiraIssue invalidIssue : invalid) {
                // Calculate IV = FV - (FV - OV) * P
                int iv = (int) (invalidIssue.getFvIndex() - invalidIssue.getFvMinusOv() * proportion);
                // Save IV
                invalidIssue.setIvIndex(iv);
                // Add current issue to the list of proportions
                lastSum += invalidIssue.calculateProportion();
                // Labeling: add issue to version corresponding to IV
                versions.get(iv).injected().add(invalidIssue);
            }
            totalIssues += version.opened().size();
        }
    }
}

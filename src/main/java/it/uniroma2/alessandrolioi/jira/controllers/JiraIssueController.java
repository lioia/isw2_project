package it.uniroma2.alessandrolioi.jira.controllers;

import it.uniroma2.alessandrolioi.jira.exceptions.JiraRestException;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;
import it.uniroma2.alessandrolioi.common.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.*;

public class JiraIssueController {
    public List<JiraIssue> loadIssues(String project, String additionalParams, LocalDate firstVersion, LocalDate lastVersion) throws JiraRestException {
        List<JiraIssue> issues = new ArrayList<>();
        int total;
        int totalDecrement = 0; // total skipped issues (missing required fields)
        int startAt = 0;
        do {
            String url = "https://issues.apache.org/jira/rest/api/2/search" +
                    "?jql=project=" + project + // selecting the project
                    " AND issueType=Bug AND(status=closed OR status=resolved)AND resolution=fixed" + // query to get all bug fix issues
                    // select issues resolved in [firstVersion, lastVersion]
                    " AND resolved>=%s AND resolved<=%s".formatted(firstVersion.toString(), lastVersion.toString()) +
                    " %s".formatted(additionalParams) +
                    "&fields=" + String.join(",", JiraIssue.getFields()) + // fields
                    "&startAt=" + startAt + // pagination offset
                    "&maxResults=1000"; // max results loaded
            // Correctly format URL
            String correctedUrl = url.replace(" ", "%20").replace(">=", "%3E%3D").replace("<=", "%3C%3D");
            // Load JSON
            JiraRestController rest = new JiraRestController();
            String json = rest.getJsonFromUrl(correctedUrl);
            JSONObject result = new JSONObject(json);
            total = result.getInt("total"); // total number of issues
            JSONArray jsonIssues = result.getJSONArray("issues");
            // Iterate through all the issues
            for (int i = 0; i < jsonIssues.length(); i++) {
                JSONObject jsonIssue = jsonIssues.getJSONObject(i);
                JSONObject fields = jsonIssue.getJSONObject("fields");
                // The issue does not have the required information, so it can be skipped
                if (!jsonIssue.has(JiraIssue.KEY_FIELD) || !fields.has(JiraIssue.RESOLUTION_DATE_FIELD) || !fields.has(JiraIssue.CREATED_FIELD)) {
                    totalDecrement += 1;
                    continue;
                }
                String key = jsonIssue.getString(JiraIssue.KEY_FIELD); // e.s. BOOKKEEPER-1
                String resolutionString = fields.getString(JiraIssue.RESOLUTION_DATE_FIELD);
                String createdString = fields.getString(JiraIssue.CREATED_FIELD);
                // Parse the dates
                LocalDate resolution = LocalDate.parse(resolutionString.substring(0, 10));
                LocalDate created = LocalDate.parse(createdString.substring(0, 10));
                // Get the highest fix version on Jira
                // Case: issue was reopened multiple times (so there are more than one fix version) ~ resolutiondate is only for the first one | BOOKKEEPER - 695
                List<LocalDate> fixVersions = getVersionsFromJsonArray(fields.getJSONArray(JiraIssue.FIX_VERSIONS_FIELD));
                // Case: multiple fix versions (after resolution date) | BOOKKEEPER-695
                Optional<LocalDate> fix = fixVersions.stream().max(Comparator.naturalOrder());
                // Replace the current resolution date to the fix version on Jira (sometimes the issue is reopened, but the resolution date is not updated)
                // Case: fix version on Jira has a release date after the created field | i.e. BOOKKEEPER-774
                //       (BOOKKEEPER-774 is not present in the issue list because it's after the last version considered)
                if (fix.isPresent() && fix.get().isAfter(created) && !fix.get().isAfter(lastVersion))
                    resolution = fix.get();

                // Get affected versions from Jira and sort them
                List<LocalDate> affectedVersions = getVersionsFromJsonArray(fields.getJSONArray(JiraIssue.VERSIONS_FIELD));
                affectedVersions.sort(Comparator.naturalOrder());

                JiraIssue issue = new JiraIssue(key, resolution, created, affectedVersions);
                issues.add(issue);
            }
            startAt += result.getInt("maxResults");
        } while (total - totalDecrement != issues.size());
        Collections.reverse(issues); // sorted by key
        return issues;
    }

    public void classifyIssues(List<JiraVersion> versions, List<JiraIssue> issues) {
        for (JiraIssue issue : issues) {
            LocalDate firstReleaseDate = versions.get(0).releaseDate();

            // Skipping the issues created and resolved before the first Jira release
            // IV, OV and FV should be the first release (it can cause problem when calculating proportion)
            if (issue.getCreated().isBefore(firstReleaseDate) && issue.getResolution().isBefore(firstReleaseDate))
                continue;

            // Find IV, OV and FV from Jira API (based on `created`, `resolution` and the first affectedVersion
            List<Pair<JiraVersion, Integer>> foundVersions = getVersions(issue, versions);
            Pair<JiraVersion, Integer> injected = foundVersions.get(0);
            Pair<JiraVersion, Integer> opening = foundVersions.get(1);
            Pair<JiraVersion, Integer> fix = foundVersions.get(2);

            // Case: affected version in Jira is after the fix version (based on resolutiondate) | i.e. BOOKKEEPER-374
            // Affected versions in Jira is incorrect, so the injected version is invalid
            if (!issue.getAffectedVersionsDates().isEmpty() && issue.getAffectedVersionsDates().get(0).isAfter(fix.first().releaseDate())) {
                issue.getAffectedVersionsDates().clear();
                injected = null;
            }

            // No injected version was found but the opening version is the first release
            // So the injected must be the first release as well
            if (injected == null && opening.first() == versions.get(0)) injected = opening;

            // Injected version is present (from affectedVersion, or derived as the first release)
            if (injected != null) {
                issue.setIvIndex(injected.second());
                injected.first().injected().add(issue);
            }
            issue.setOvIndex(opening.second());
            issue.setFvIndex(fix.second());

            opening.first().opened().add(issue);
            fix.first().fixed().add(issue);
        }
    }

    private List<LocalDate> getVersionsFromJsonArray(JSONArray array) {
        List<LocalDate> versions = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            if (!o.has(JiraVersion.RELEASE_DATE_FIELD)) continue;
            String dateString = o.getString(JiraVersion.RELEASE_DATE_FIELD);
            LocalDate date = LocalDate.parse(dateString);
            versions.add(date);
        }
        return versions;
    }


    private List<Pair<JiraVersion, Integer>> getVersions(JiraIssue issue, List<JiraVersion> versions) {
        Pair<JiraVersion, Integer> injected = null;
        Pair<JiraVersion, Integer> opening = null;
        Pair<JiraVersion, Integer> fix = null;

        for (int i = 0; i < versions.size(); i++) {
            JiraVersion version = versions.get(i);
            // Injected version is the first affected version, if present
            if (injected == null && !issue.getAffectedVersionsDates().isEmpty() && issue.getAffectedVersionsDates().get(0).isEqual(version.releaseDate()))
                injected = new Pair<>(version, i);
            // Opening version is set as the first release after the jira ticket was created
            if (opening == null && version.releaseDate().isAfter(issue.getCreated())) opening = new Pair<>(version, i);
            // Fix version is set as the first release after the jira ticket was set as resolved
            if (fix == null && !version.releaseDate().isBefore(issue.getResolution()))
                fix = new Pair<>(version, i);
            // All variables are set, it is not necessary to search the whole list
            if (injected != null && opening != null && fix != null) break;
        }

        return Arrays.asList(injected, opening, fix);
    }
}

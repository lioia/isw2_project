package it.uniroma2.alessandrolioi.jira;

import it.uniroma2.alessandrolioi.jira.exceptions.JiraRESTException;
import it.uniroma2.alessandrolioi.jira.models.JiraCompleteIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;
import it.uniroma2.alessandrolioi.utils.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.stream;

public class Jira {
    private final String project;

    public Jira(String project) {
        this.project = project.toUpperCase();
    }

    public List<JiraVersion> loadVersions() throws JiraRESTException {
        List<JiraVersion> versions = new ArrayList<>();
        String url = "https://issues.apache.org/jira/rest/api/2/project/%s/versions".formatted(project);
        String json = getJsonFromUrl(url);
        JSONArray jsonVersions = new JSONArray(json);
        // Loading only 50% of releases (for better dataset accuracy ~ snoring problem)
        for (int i = 0; i < Math.ceilDiv(jsonVersions.length(), 2); i++) {
            JSONObject jsonVersion = jsonVersions.getJSONObject(i);
            String id = jsonVersion.getString("id");
            String name = jsonVersion.getString("name");
            // Skipping versions that do not have a release date (only required field)
            if (!jsonVersion.has("releaseDate")) continue;
            String releaseDateString = jsonVersion.getString("releaseDate");
            LocalDate date = LocalDate.parse(releaseDateString);
            JiraVersion version = new JiraVersion(id, name, date);
            versions.add(version);
        }

        versions.sort(Comparator.comparing(JiraVersion::releaseDate));

        return versions;
    }

    public List<JiraIssue> loadIssues(LocalDate lastVersion) throws JiraRESTException {
        List<JiraIssue> issues = new ArrayList<>();
        int total;
        int totalDecrement = 0; // total skipped issues (missing required fields)
        int startAt = 0;
        do {
            String url = "https://issues.apache.org/jira/rest/api/2/search" +
                    "?jql=project=" + project + // selecting the project
                    " AND issueType=Bug AND(status=closed OR status=resolved)AND resolution=fixed" + // query to get all bug fix issues
                    "&fields=key,versions,fixVersions,resolutiondate,created" + // fields
                    "&startAt=" + startAt + // pagination offset
                    "&maxResults=1000"; // max results loaded
            String json = getJsonFromUrl(url.replace(" ", "%20")); // Correctly format URL and get resulting json
            JSONObject result = new JSONObject(json);
            total = result.getInt("total"); // total number of issues
            JSONArray jsonIssues = result.getJSONArray("issues");
            // Iterate through all the issues
            for (int i = 0; i < jsonIssues.length(); i++) {
                JSONObject jsonIssue = jsonIssues.getJSONObject(i);
                JSONObject fields = jsonIssue.getJSONObject("fields");
                // The issue does not have the required information, so it can be skipped
                if (!jsonIssue.has("key") || !fields.has("resolutiondate") || !fields.has("created")) {
                    totalDecrement += 1;
                    continue;
                }
                String key = jsonIssue.getString("key"); // e.s. BOOKKEEPER-1
                String resolutionString = fields.getString("resolutiondate");
                String createdString = fields.getString("created");
                // Parse the dates
                LocalDate resolution = LocalDate.parse(resolutionString.substring(0, 10));
                LocalDate created = LocalDate.parse(createdString.substring(0, 10));
                // Skipping issues created after the last version considered
                if(created.isAfter(lastVersion)) {
                    totalDecrement += 1;
                    continue;
                }
                // Get the highest fix version on Jira
                // Case: issue was reopened multiple times (so there are more than one fix version) ~ resolutiondate is only for the first one | BOOKKEEPER - 695
                List<LocalDate> fixVersions = getVersionsFromJsonArray(fields.getJSONArray("fixVersions"));
                // Case: multiple fix versions (after resolution date) | BOOKKEEPER-695
                Optional<LocalDate> fix = fixVersions.stream().max(Comparator.naturalOrder());
                // Replace the current resolution date to the fix version on Jira (sometimes the issue is reopened, but the resolution date is not updated)
                // Case: fix version on Jira has a release date after the created field | i.e. BOOKKEEPER-774
                if (fix.isPresent() && fix.get().isAfter(created)) resolution = fix.get();

                // Skipping issues resolved after the last version considered
                if (resolution.isAfter(lastVersion)) {
                    totalDecrement += 1;
                    continue;
                }

                // Get affected versions from Jira and sort them
                List<LocalDate> affectedVersions = getVersionsFromJsonArray(fields.getJSONArray("versions"));
                affectedVersions.sort(Comparator.naturalOrder());

                JiraIssue issue = new JiraIssue(key, resolution, created, affectedVersions);
                issues.add(issue);
            }
            startAt += result.getInt("maxResults");
        } while (total - totalDecrement != issues.size());
        Collections.reverse(issues); // sorted by key
        return issues;
    }

    // TODO injected can still be null, apply proportion
    public List<JiraCompleteIssue> getCompleteIssues(List<JiraVersion> versions, List<JiraIssue> issues) {
        List<JiraCompleteIssue> completeIssues = new ArrayList<>();
        for (JiraIssue issue : issues) {
            LocalDate firstReleaseDate = versions.get(0).releaseDate();

            // Skipping the issues created and resolved before the first Jira release
            // IV, OV and FV should be the first release (it can cause problem when calculating proportion)
            if (issue.getCreated().isBefore(firstReleaseDate) && issue.getResolution().isBefore(firstReleaseDate))
                continue;

            // Find IV, OV and FV from Jira API (based on `created`, `resolution` and the first affectedVersion
            List<Pair<JiraVersion, Integer>> foundVersions = getVersions(issue, versions);
            Pair<JiraVersion, Integer> injected = foundVersions.get(0);
            JiraVersion injectedVersion = null;
            if (injected != null) injectedVersion = injected.first();
            Pair<JiraVersion, Integer> opening = foundVersions.get(1);
            Pair<JiraVersion, Integer> fix = foundVersions.get(2);

            // Case: affected version in Jira is after the fix version (based on resolutiondate) | i.e. BOOKKEEPER-374
            // Affected versions in Jira is incorrect, so the injected version is invalid
            if (!issue.getAffectedVersionsDates().isEmpty() && issue.getAffectedVersionsDates().get(0).isAfter(fix.first().releaseDate())) {
                issue.getAffectedVersionsDates().clear();
                injected = null;
                injectedVersion = null;
            }

            int fvOvDifference = fix.second() - opening.second(); // This should always be present
            int fvIvDifference = 0; // This can be 0 if there is no injected version

            // from Proportion paper step d.3 on page 13 (so the denominator won't cause any problems)
            if (fvOvDifference == 0 && opening.first() != versions.get(0)) fvOvDifference = 1;

            // No injected version was found but the opening version is the first release
            // So the injected must be the first release as well
            if (injected == null && opening.first() == versions.get(0)) injectedVersion = opening.first();

            // If the injected version is present (from affectedVersion, or it can be derived as the first release)
            // The FV-IV can be calculated
            if (injected != null)
                fvIvDifference = fix.second() - injected.second();

            JiraCompleteIssue complete = new JiraCompleteIssue(issue, injectedVersion, opening.first(), fix.first(), fvOvDifference, fvIvDifference);
            completeIssues.add(complete);
        }
        return completeIssues;
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
            if (fix == null && (version.releaseDate().isAfter(issue.getResolution()) || version.releaseDate().isEqual(issue.getResolution())))
                fix = new Pair<>(version, i);
            // All variables are set, it is not necessary to search the whole list
            if (injected != null && opening != null && fix != null) break;
        }

        return Arrays.asList(injected, opening, fix);
    }

    private List<LocalDate> getVersionsFromJsonArray(JSONArray array) {
        List<LocalDate> versions = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            if (!o.has("releaseDate")) continue;
            String dateString = o.getString("releaseDate");
            LocalDate date = LocalDate.parse(dateString);
            versions.add(date);
        }
        return versions;
    }

    private String getJsonFromUrl(String url) throws JiraRESTException {
        try (InputStream stream = URI.create(url).toURL().openStream()) {
            byte[] bytes = stream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (MalformedURLException e) {
            throw new JiraRESTException("Incorrect url: %s".formatted(url), e);
        } catch (IOException e) {
            throw new JiraRESTException("Could not load page: %s".formatted(url), e);
        }
    }
}

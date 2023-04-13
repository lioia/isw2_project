package it.uniroma2.alessandrolioi.jira;

import it.uniroma2.alessandrolioi.jira.exceptions.JiraRESTException;
import it.uniroma2.alessandrolioi.jira.models.JiraCompleteIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraIssue;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

// TODO remove 50% of the dataset
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
        for (int i = 0; i < jsonVersions.length(); i++) {
            JSONObject jsonVersion = jsonVersions.getJSONObject(i);
            String id = jsonVersion.getString("id");
            String name = jsonVersion.getString("name");
            String releaseDateString = jsonVersion.getString("releaseDate");
            // Skipping versions that do not have a release date (only required field)
            if (releaseDateString == null) continue;
            LocalDate date = LocalDate.parse(releaseDateString);
            JiraVersion version = new JiraVersion(id, name, date);
            versions.add(version);
        }

        versions.sort(Comparator.comparing(JiraVersion::releaseDate));

        return versions;
    }

    public List<JiraIssue> loadIssues() throws JiraRESTException {
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
            String json = getJsonFromUrl(url.replace(" ", "%20"));
            JSONObject result = new JSONObject(json);
            total = result.getInt("total");
            JSONArray jsonIssues = result.getJSONArray("issues");
            for (int i = 0; i < jsonIssues.length(); i++) {
                JSONObject jsonIssue = jsonIssues.getJSONObject(i);
                JSONObject fields = jsonIssue.getJSONObject("fields");
                String key = jsonIssue.getString("key");
                String resolutionString = fields.getString("resolutiondate");
                String createdString = fields.getString("created");
                if (key == null || resolutionString == null || createdString == null) {
                    totalDecrement += 1;
                    continue;
                }
                LocalDate resolution = LocalDate.parse(resolutionString.substring(0, 10));
                LocalDate created = LocalDate.parse(createdString.substring(0, 10));
                List<LocalDate> affectedVersions = new ArrayList<>();
                JSONArray versions = fields.getJSONArray("versions");
                for (int j = 0; j < versions.length(); j++) {
                    JSONObject v = versions.getJSONObject(j);
                    String date = v.getString("releaseDate");
                    if (date == null) continue;
                    affectedVersions.add(LocalDate.parse(date));
                }
                affectedVersions.sort(Comparator.naturalOrder()); // sort list
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
            JiraVersion[] foundVersions = getVersions(issue, versions);
            JiraVersion injected = foundVersions[0];
            JiraVersion opening = foundVersions[1];
            JiraVersion fix = foundVersions[2];

            // There is no version that can be tagged as injected before this
            if (injected == null && opening == versions.get(0)) injected = opening;

            JiraCompleteIssue complete = new JiraCompleteIssue(issue, injected, opening, fix);
            completeIssues.add(complete);
        }
        return completeIssues;
    }

    private JiraVersion[] getVersions(JiraIssue issue, List<JiraVersion> versions) {
        JiraVersion injected = null;
        JiraVersion opening = null;
        JiraVersion fix = null;

        for (JiraVersion version : versions) {
            // Injected version is the first affected version, if present
            if (!issue.getAffectedVersionsDates().isEmpty() && issue.getAffectedVersionsDates().get(0).isEqual(version.releaseDate()))
                injected = version;
            // Opening version is set as the first release after the jira ticket was created
            if (opening == null && version.releaseDate().isAfter(issue.getCreated())) opening = version;
            // Fix version is set as the first release after the jira ticket was set as resolved
            if (fix == null && version.releaseDate().isAfter(issue.getResolution())) fix = version;
            // All variables are set, it is not necessary to search the whole list
            if (injected != null && opening != null && fix != null) break;
        }

        return new JiraVersion[]{injected, opening, fix};
    }

    private String getJsonFromUrl(String url) throws JiraRESTException {
        try (InputStream stream = URI.create(url).toURL().openStream()) {
            byte[] bytes = stream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (MalformedURLException e) {
            throw new JiraRESTException("Incorrect url: %s".formatted(url), e);
        } catch (IOException e) {
            throw new JiraRESTException("Could not load page", e);
        }
    }
}

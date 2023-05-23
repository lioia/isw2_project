package it.uniroma2.alessandrolioi.jira.controllers;

import it.uniroma2.alessandrolioi.jira.exceptions.JiraRestException;
import it.uniroma2.alessandrolioi.jira.models.JiraVersion;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JiraVersionController {
    public List<JiraVersion> loadVersions(String project) throws JiraRestException {
        List<JiraVersion> versions = new ArrayList<>();
        String url = "https://issues.apache.org/jira/rest/api/2/project/%s/versions".formatted(project.toUpperCase());
        JiraRestController rest = new JiraRestController();
        String json = rest.getJsonFromUrl(url);
        JSONArray jsonVersions = new JSONArray(json);
        for (int i = 0; i < jsonVersions.length(); i++) {
            JSONObject jsonVersion = jsonVersions.getJSONObject(i);
            boolean released = jsonVersion.getBoolean(JiraVersion.RELEASED_FIELD);
            // Skipping versions that do not have a release date (only required field) or are set as not released
            if (!jsonVersion.has(JiraVersion.RELEASE_DATE_FIELD) || !released) continue;
            String name = jsonVersion.getString(JiraVersion.NAME_FIELD);
            String releaseDateString = jsonVersion.getString(JiraVersion.RELEASE_DATE_FIELD);
            LocalDate date = LocalDate.parse(releaseDateString);
            JiraVersion version = new JiraVersion(name, date);
            versions.add(version);
        }

        versions.sort(Comparator.comparing(JiraVersion::releaseDate));

        // Loading only 50% of releases (for better dataset accuracy ~ snoring problem)
        int numberOfVersions = (int) (((float) versions.size()) / 2);
        return versions.subList(0, numberOfVersions);
    }
}

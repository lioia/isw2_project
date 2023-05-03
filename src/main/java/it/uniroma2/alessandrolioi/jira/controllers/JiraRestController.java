package it.uniroma2.alessandrolioi.jira.controllers;

import it.uniroma2.alessandrolioi.jira.exceptions.JiraRestException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class JiraRestController {
    public String getJsonFromUrl(String url) throws JiraRestException {
        try (InputStream stream = URI.create(url).toURL().openStream()) {
            byte[] bytes = stream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (MalformedURLException e) {
            throw new JiraRestException("Incorrect url: %s".formatted(url), e);
        } catch (IOException e) {
            throw new JiraRestException("Could not load page: %s".formatted(url), e);
        }
    }
}

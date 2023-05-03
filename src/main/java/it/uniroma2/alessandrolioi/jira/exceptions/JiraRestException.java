package it.uniroma2.alessandrolioi.jira.exceptions;

import java.io.Serial;

public class JiraRestException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public JiraRestException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

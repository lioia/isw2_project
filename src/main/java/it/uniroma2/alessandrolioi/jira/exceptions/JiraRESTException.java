package it.uniroma2.alessandrolioi.jira.exceptions;

import java.io.Serial;
import java.net.MalformedURLException;

public class JiraRESTException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public JiraRESTException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

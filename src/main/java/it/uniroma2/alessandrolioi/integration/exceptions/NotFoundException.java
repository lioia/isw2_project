package it.uniroma2.alessandrolioi.integration.exceptions;

import java.io.Serial;

public class NotFoundException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(String version) {
        super("Commit for version %s was not found".formatted(version));
    }
}

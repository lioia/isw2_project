package it.uniroma2.alessandrolioi.git.exceptions;

import java.io.Serial;

public class GitFileException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public GitFileException(String reason, Throwable cause) {
        super("[GIT-FILE] %s".formatted(reason), cause);
    }
}

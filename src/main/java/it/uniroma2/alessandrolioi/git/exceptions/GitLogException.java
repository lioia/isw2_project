package it.uniroma2.alessandrolioi.git.exceptions;

import java.io.Serial;

public class GitLogException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public GitLogException(String reason, Throwable cause) {
        super("[LOG] %s".formatted(reason), cause);
    }
}

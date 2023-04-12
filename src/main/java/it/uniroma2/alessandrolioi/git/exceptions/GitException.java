package it.uniroma2.alessandrolioi.git.exceptions;

import java.io.Serial;

public class GitException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public GitException(String step, String reason) {
        super("[%s] %s".formatted(step, reason));
    }

    public GitException(String step, Throwable cause) {
        super("[%s]".formatted(step), cause);
    }

    public GitException(String step, String reason, Throwable cause) {
        super("[%s] %s".formatted(step, reason), cause);
    }
}

package it.uniroma2.alessandrolioi.git.exceptions;

import java.io.Serial;

public class GitRepoException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public GitRepoException(String reason) {
        super("[REPO] %s".formatted(reason));
    }

    public GitRepoException(String reason, Throwable cause) {
        super("[REPO] %s".formatted(reason), cause);
    }
}

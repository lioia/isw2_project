package it.uniroma2.alessandrolioi.git.exceptions;

import java.io.Serial;

public class GitDiffException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public GitDiffException(String reason, Exception e) {
        super("[DIFF] %s".formatted(reason), e);
    }
}

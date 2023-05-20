package it.uniroma2.alessandrolioi.dataset.exceptions;

import it.uniroma2.alessandrolioi.git.exceptions.GitLogException;

import java.io.Serial;

public class MetricException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public MetricException(Throwable cause) {
        super(cause);
    }

    public MetricException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

package it.uniroma2.alessandrolioi.dataset.exceptions;

import java.io.Serial;

public class MetricException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public MetricException(String metric, String reason, Throwable cause) {
        super("[METRIC] %s failed: %s".formatted(metric, reason), cause);
    }
}

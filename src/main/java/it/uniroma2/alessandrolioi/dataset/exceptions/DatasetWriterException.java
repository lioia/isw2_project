package it.uniroma2.alessandrolioi.dataset.exceptions;

import java.io.Serial;

public class DatasetWriterException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public DatasetWriterException(String reason, Throwable cause) {
        super("[WRITE] %s".formatted(reason), cause);
    }

    public DatasetWriterException(Throwable cause) {
        super(cause);
    }
}

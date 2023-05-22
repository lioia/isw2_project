package it.uniroma2.alessandrolioi.analysis.exceptions;

public class CsvException extends Exception {
    public CsvException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

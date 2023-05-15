package it.uniroma2.alessandrolioi.analysis.exceptions;

public class CsvException extends Exception {
    public CsvException(String reason) {
        super(reason);
    }

    public CsvException(String reason, Throwable e) {
        super(reason, e);
    }
}

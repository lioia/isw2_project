package it.uniroma2.alessandrolioi.dataset.exceptions;

public class BuggyException extends Exception {
    public BuggyException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

package it.uniroma2.alessandrolioi.analysis.exceptions;

public class EvaluationException extends Throwable {
    public EvaluationException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

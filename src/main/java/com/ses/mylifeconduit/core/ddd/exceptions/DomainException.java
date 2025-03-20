package com.ses.mylifeconduit.core.ddd.exceptions;

/**
 * Base exception class for domain-specific errors.
 */
public class DomainException extends Exception {
    private final String errorCode;
    private final String context;

    /**
     * Constructs a new DomainException.
     * @param message Descriptive message of the error
     * @param errorCode Unique code identifying the error type
     * @param context Additional context (e.g., aggregate ID or event type)
     */
    public DomainException(String message, String errorCode, String context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getContext() {
        return context;
    }
}
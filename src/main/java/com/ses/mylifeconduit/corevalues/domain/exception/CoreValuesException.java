package com.ses.mylifeconduit.corevalues.domain.exception;

/**
 * Base exception for errors within the Core Values domain.
 */
public abstract class CoreValuesException extends RuntimeException {
    public CoreValuesException(String message) {
        super(message);
    }
}
package com.ses.mylifeconduit.core.ddd.exceptions;

/**
 * Exception thrown when an aggregate's invariants are violated.
 */
public class InvariantViolationException extends DomainException {

    /**
     * Constructs a new InvariantViolationException.
     * @param message The detail message.
     */
    public InvariantViolationException(String message) {
        super(message, "INVARIANT_VIOLATION", "Aggregate state update");
    }
}
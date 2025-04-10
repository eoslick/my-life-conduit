package com.ses.mylifeconduit.corevalues.domain.vo;

import com.ses.mylifeconduit.core.ddd.ValueObject;
import com.ses.mylifeconduit.corevalues.domain.exception.InvalidCoreValueTextException;

import java.util.Objects;

public final record CoreValueText(String value) implements ValueObject {

    private static final int MAX_LENGTH = 100; // Example max length

    /**
     * Creates CoreValueText, validating the input string.
     * @param value The text content.
     * @throws InvalidCoreValueTextException if validation fails.
     */
    public CoreValueText {
        Objects.requireNonNull(value, "CoreValueText value cannot be null");
        value = value.trim(); // Trim whitespace
        if (value.isEmpty()) {
            throw new InvalidCoreValueTextException("Core value text cannot be empty.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidCoreValueTextException("Core value text cannot exceed " + MAX_LENGTH + " characters.");
        }
        // Potentially add other validation (e.g., character checks)
    }

    @Override
    public String toString() {
        return value;
    }
    // equals() and hashCode() are automatically generated by the record (case-sensitive by default)
    // Consider implementing custom equals/hashCode for case-insensitive comparison if needed.
}
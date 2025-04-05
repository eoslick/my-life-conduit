package com.ses.mylifeconduit.goalsetting.domain.vo;

import com.ses.mylifeconduit.core.ddd.ValueObject;
// Assuming a specific exception type is desired
import com.ses.mylifeconduit.goalsetting.domain.exception.InvalidGoalDescriptionException;
import java.util.Objects;

/**
 * Represents the description text of a Goal.
 * Enforces validation rules (e.g., non-empty, length limits).
 * Implements ValueObject marker interface.
 */
public final record GoalDescription(String value) implements ValueObject {

    private static final int MAX_LENGTH = 500; // Example max length

    /**
     * Creates GoalDescription, validating the input string.
     * @param value The description text.
     * @throws InvalidGoalDescriptionException if validation fails.
     */
    public GoalDescription {
        Objects.requireNonNull(value, "GoalDescription value cannot be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidGoalDescriptionException("Goal description cannot be empty.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidGoalDescriptionException("Goal description cannot exceed " + MAX_LENGTH + " characters.");
        }
        // Add any other relevant validation
    }

    @Override
    public String toString() {
        return value;
    }
    // equals() and hashCode() are automatically generated
}
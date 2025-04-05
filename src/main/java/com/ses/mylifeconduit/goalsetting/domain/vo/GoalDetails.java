// --- File: com/ses/mylifeconduit/goalsetting/domain/vo/GoalDetails.java ---

package com.ses.mylifeconduit.goalsetting.domain.vo;

import com.ses.mylifeconduit.core.ddd.ValueObject; // Mark as VO

import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object representing the detailed state of a single Goal
 * within the UserGoals aggregate. Includes identity, content, and structure.
 */
public record GoalDetails(
        GoalId id,
        Circle circle,
        Timeframe timeframe,
        GoalDescription description,
        GoalId parentGoalId // Nullable: Null for root goals (e.g., LongTerm)
) implements ValueObject, Serializable { // Also implement Serializable for event payloads

    // Using canonical constructor for basic non-null validation
    public GoalDetails {
        Objects.requireNonNull(id, "GoalDetails: id cannot be null");
        Objects.requireNonNull(circle, "GoalDetails: circle cannot be null");
        Objects.requireNonNull(timeframe, "GoalDetails: timeframe cannot be null");
        Objects.requireNonNull(description, "GoalDetails: description cannot be null");
        // parentGoalId can be null
    }

    // No additional methods needed currently, rely on record's generated methods.
    // equals(), hashCode(), toString() are value-based.
}
// --- End File ---
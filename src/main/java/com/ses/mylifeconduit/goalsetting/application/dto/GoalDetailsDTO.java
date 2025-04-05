package com.ses.mylifeconduit.goalsetting.application.dto;

import java.util.Objects;

/**
 * DTO representing the details of a single Goal.
 */
public record GoalDetailsDTO(
        String id,          // String representation of GoalId (UUID)
        String circle,      // Name/Display Name of the Circle enum
        String timeframe,   // Name of the Timeframe enum
        String description,
        String parentGoalId // String representation of parent GoalId (UUID), or null
) {
    public GoalDetailsDTO {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(circle, "circle cannot be null");
        Objects.requireNonNull(timeframe, "timeframe cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        // parentGoalId can be null
    }
}
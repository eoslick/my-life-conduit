package com.ses.mylifeconduit.goalsetting.application.dto;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * DTO representing a user's complete set of goals and priorities.
 */
public record UserGoalsDTO(
        String userId,              // String representation of UserId (UUID)
        List<GoalDetailsDTO> allGoals, // A flat list of all defined goals
        Set<String> priorityGoalIds  // Set of GoalId strings marked as priority
) {
    /**
     * Canonical constructor ensuring collections are not null.
     */
    public UserGoalsDTO {
        Objects.requireNonNull(userId, "userId cannot be null");
        // Ensure collections are unmodifiable and never null
        allGoals = (allGoals != null) ? List.copyOf(allGoals) : List.of();
        priorityGoalIds = (priorityGoalIds != null) ? Set.copyOf(priorityGoalIds) : Set.of();
    }

    /**
     * Convenience factory for when user goals are not found or not initialized.
     */
    public static UserGoalsDTO notFound(String userId) {
        return new UserGoalsDTO(userId, List.of(), Set.of());
    }
}
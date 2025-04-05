package com.ses.mylifeconduit.goalsetting.application.exception;

import com.ses.mylifeconduit.core.user.UserId;

/**
 * Exception indicating that UserGoals for a specific user were not found
 * when they were expected to exist (e.g., for breakdown).
 */
public class UserGoalsNotFoundException extends RuntimeException {
    public UserGoalsNotFoundException(UserId userId) {
        super("User goals not found for user: " + userId.value().toString());
    }
}
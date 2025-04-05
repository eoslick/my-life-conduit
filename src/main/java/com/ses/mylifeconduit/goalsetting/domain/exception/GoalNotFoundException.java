package com.ses.mylifeconduit.goalsetting.domain.exception;

import com.ses.mylifeconduit.goalsetting.domain.vo.GoalId;

public final class GoalNotFoundException extends GoalSettingException {
    public GoalNotFoundException(GoalId goalId) {
        super("Goal with ID " + goalId.value().toString() + " not found.");
    }
    public GoalNotFoundException(String message) { // Overload for different contexts
        super(message);
    }
}
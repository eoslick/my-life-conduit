package com.ses.mylifeconduit.goalsetting.domain.exception;

import com.ses.mylifeconduit.goalsetting.domain.vo.GoalId;

/**
 * Exception indicating an attempt to define a goal with an ID that already exists
 * within the user's goal set.
 */
public final class GoalIdAlreadyExistsException extends GoalSettingException {
    public GoalIdAlreadyExistsException(GoalId goalId) {
        super("Goal with ID " + (goalId != null ? goalId.value().toString() : "null") + " already exists.");
    }
}
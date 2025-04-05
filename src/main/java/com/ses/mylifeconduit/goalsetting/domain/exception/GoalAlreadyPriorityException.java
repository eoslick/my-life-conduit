package com.ses.mylifeconduit.goalsetting.domain.exception;

import com.ses.mylifeconduit.goalsetting.domain.vo.GoalId;

public final class GoalAlreadyPriorityException extends GoalSettingException {
    public GoalAlreadyPriorityException(GoalId goalId) {
        super("Goal with ID " + goalId.value().toString() + " is already a priority goal.");
    }
}
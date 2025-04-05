package com.ses.mylifeconduit.goalsetting.domain.exception;

import com.ses.mylifeconduit.goalsetting.domain.vo.GoalId;

public final class GoalNotPriorityException extends GoalSettingException {
    public GoalNotPriorityException(GoalId goalId) {
        super("Goal with ID " + goalId.value().toString() + " is not currently a priority goal.");
    }
}
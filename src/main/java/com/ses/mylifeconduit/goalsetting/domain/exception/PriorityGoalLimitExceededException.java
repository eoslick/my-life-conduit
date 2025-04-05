package com.ses.mylifeconduit.goalsetting.domain.exception;

public final class PriorityGoalLimitExceededException extends GoalSettingException {
    public PriorityGoalLimitExceededException(int limit) {
        super("Cannot select priority goal: Limit of " + limit + " already reached.");
    }
}
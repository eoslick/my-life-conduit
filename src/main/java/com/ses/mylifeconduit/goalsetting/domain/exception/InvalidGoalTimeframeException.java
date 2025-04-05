package com.ses.mylifeconduit.goalsetting.domain.exception;

import com.ses.mylifeconduit.goalsetting.domain.vo.Timeframe;

public final class InvalidGoalTimeframeException extends GoalSettingException {
    public InvalidGoalTimeframeException(String message) {
        super(message);
    }
    public InvalidGoalTimeframeException(Timeframe requiredTimeframe) {
        super("Action requires goal to have timeframe: " + requiredTimeframe.name());
    }
}
package com.ses.mylifeconduit.goalsetting.domain.exception;

/**
 * Base exception for errors within the Goal Setting domain.
 */
public abstract class GoalSettingException extends RuntimeException {
    public GoalSettingException(String message) {
        super(message);
    }
    public GoalSettingException(String message, Throwable cause) {
        super(message, cause);
    }
}
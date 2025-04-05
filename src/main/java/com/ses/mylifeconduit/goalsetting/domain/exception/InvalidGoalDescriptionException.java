package com.ses.mylifeconduit.goalsetting.domain.exception;

/**
 * Exception indicating an invalid goal description was provided.
 */
public class InvalidGoalDescriptionException extends RuntimeException { // Or extend a base GoalSettingException
    public InvalidGoalDescriptionException(String message) {
        super(message);
    }
}
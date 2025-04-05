package com.ses.mylifeconduit.corevalues.application.exception;

import com.ses.mylifeconduit.core.user.UserId;

/**
 * Exception indicating that UserCoreValues for a specific user were not found.
 * (Example of an application-level exception if needed, distinct from domain exceptions).
 */
public class CoreValuesNotFoundException extends RuntimeException {
    public CoreValuesNotFoundException(UserId userId) {
        super("Core values not found for user: " + userId.value().toString());
    }
}
// src/main/java/com/ses/mylifeconduit/core/security/exception/AccessDeniedException.java
package com.ses.mylifeconduit.core.security.exception;

/**
 * Exception thrown when an operation is denied due to insufficient permissions
 * (e.g., user trying to access data without a valid key or share grant).
 */
public class AccessDeniedException extends SecurityCoreException {
    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
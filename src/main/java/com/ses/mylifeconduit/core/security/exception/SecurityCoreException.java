// src/main/java/com/ses/mylifeconduit/core/security/exception/SecurityCoreException.java
package com.ses.mylifeconduit.core.security.exception;

/**
 * Base exception for all security-related errors originating from the core security components
 * (e.g., EncryptionService, KeyManagementService).
 */
public class SecurityCoreException extends RuntimeException {

    public SecurityCoreException(String message) {
        super(message);
    }

    public SecurityCoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
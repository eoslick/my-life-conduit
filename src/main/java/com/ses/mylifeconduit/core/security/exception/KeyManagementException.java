// src/main/java/com/ses/mylifeconduit/core/security/exception/KeyManagementException.java
package com.ses.mylifeconduit.core.security.exception;

/**
 * Exception thrown for errors related to key management operations
 * (e.g., wrapping/unwrapping keys, resolving keys, accessing key store).
 */
public class KeyManagementException extends SecurityCoreException {
    public KeyManagementException(String message) {
        super(message);
    }

    public KeyManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
// src/main/java/com/ses/mylifeconduit/core/security/exception/EncryptionException.java
package com.ses.mylifeconduit.core.security.exception;

/**
 * Exception thrown when an error occurs during data encryption.
 */
public class EncryptionException extends SecurityCoreException {
    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
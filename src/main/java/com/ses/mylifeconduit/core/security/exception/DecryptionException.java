// src/main/java/com/ses/mylifeconduit/core/security/exception/DecryptionException.java
package com.ses.mylifeconduit.core.security.exception;

/**
 * Exception thrown when an error occurs during data decryption
 * (e.g., incorrect key, corrupted data, algorithm mismatch).
 */
public class DecryptionException extends SecurityCoreException {
    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
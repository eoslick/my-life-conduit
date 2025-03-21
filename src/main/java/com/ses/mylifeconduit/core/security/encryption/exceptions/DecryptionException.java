package com.ses.mylifeconduit.core.security.encryption.exceptions;

import com.ses.mylifeconduit.core.ddd.exceptions.DomainException;

/**
 * Exception thrown when decryption of data fails.
 * This could be due to invalid key, corrupted data, or tampering.
 */
public class DecryptionException extends DomainException {
    
    /**
     * Creates a new decryption exception.
     * 
     * @param message The exception message
     * @param cause The underlying cause of the exception
     */
    public DecryptionException(String message, Throwable cause) {
        super(message, "DECRYPTION_FAILED", "Security", cause);
    }
    
    /**
     * Creates a new decryption exception.
     * 
     * @param message The exception message
     */
    public DecryptionException(String message) {
        super(message, "DECRYPTION_FAILED", "Security");
    }
}
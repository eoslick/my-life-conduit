package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.security.encryption.exceptions.DecryptionException;

/**
 * Interface for encryption algorithms that can be plugged into the system.
 * Implementations should handle the actual encryption/decryption operations.
 */
public interface EncryptionAlgorithm {
    
    /**
     * Gets the metadata identifier for this encryption algorithm.
     * This ID is stored with encrypted data to track which algorithm was used.
     * 
     * @return The algorithm's metadata identifier
     */
    EncryptionMetadataId getMetadataId();
    
    /**
     * Encrypts data using the provided key.
     * 
     * @param data The plaintext data to encrypt
     * @param key The encryption key
     * @return The encrypted data
     */
    byte[] encrypt(byte[] data, byte[] key);
    
    /**
     * Decrypts data using the provided key.
     * 
     * @param encryptedData The encrypted data
     * @param key The decryption key
     * @return The decrypted data
     * @throws DecryptionException if decryption fails
     */
    byte[] decrypt(byte[] encryptedData, byte[] key) throws DecryptionException;
}
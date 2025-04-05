// src/main/java/com/ses/mylifeconduit/core/encryption/EncryptionService.java
package com.ses.mylifeconduit.core.encryption;

// Corrected import statement:
import com.ses.mylifeconduit.core.security.exception.SecurityCoreException;

import java.io.Serializable;

/**
 * Service interface for encrypting and decrypting data.
 * Implementations provide specific algorithms (e.g., AES/GCM, NoOp).
 * <p>
 * Implementations may need to collaborate with the
 * {@link com.ses.mylifeconduit.core.security.KeyManagementService}
 * to resolve the actual cryptographic keys based on the provided {@link KeyContext}
 * or the metadata within {@link EncryptedValue}.
 */
public interface EncryptionService {

    /**
     * Encrypts the given plaintext value using the provided key context.
     *
     * @param <T>        The type of the plaintext value, must be Serializable.
     * @param plainValue The plaintext object to encrypt.
     * @param context    The {@link KeyContext} containing tenant, user, and unique context ID.
     *                   This context is used (potentially via KMS) to determine the correct
     *                   encryption key and details for this operation.
     * @return An {@link EncryptedValue} containing the ciphertext and necessary metadata.
     * @throws SecurityCoreException If encryption fails (e.g., key unavailable, algorithm error).
     */
    <T extends Serializable> EncryptedValue<T> encrypt(T plainValue, KeyContext context);

    /**
     * Decrypts the given {@link EncryptedValue} back to its original plaintext object.
     * <p>
     * The implementation will use the {@code algorithmId} and {@code keyContextId}
     * from the {@code encryptedValue} metadata, likely collaborating with the KMS
     * to retrieve the appropriate decryption key.
     *
     * @param <T>            The expected type of the decrypted plaintext object.
     * @param encryptedValue The {@link EncryptedValue} containing ciphertext and metadata.
     * @return The original plaintext object.
     * @throws SecurityCoreException If decryption fails (e.g., key unavailable, wrong key,
     *                               tampered data, algorithm error, class cast issue).
     */
    <T extends Serializable> T decrypt(EncryptedValue<T> encryptedValue);

    /**
     * Gets the unique identifier for the encryption algorithm implemented by this service.
     * This ID should match one of the constants defined in {@link EncryptionAlgorithm}.
     *
     * @return The algorithm identifier string.
     */
    String getAlgorithmId();
}
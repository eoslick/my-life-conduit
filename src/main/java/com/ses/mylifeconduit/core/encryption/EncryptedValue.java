// src/main/java/com/ses/mylifeconduit/core/encryption/EncryptedValue.java
package com.ses.mylifeconduit.core.encryption;

import com.ses.mylifeconduit.core.ddd.ValueObject;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * A Value Object representing encrypted data along with necessary metadata for decryption.
 * This ensures that the context needed to decrypt (algorithm, key reference) travels
 * with the ciphertext.
 *
 * @param <T> The original type of the data that has been encrypted. Used for type safety
 *            but the data itself is held as bytes.
 */

public record EncryptedValue<T extends Serializable>(
        byte[] encryptedData,    // The raw encrypted ciphertext
        String algorithmId,        // Identifier for the encryption algorithm used (e.g., "AES_GCM_256")
        UUID dekContextId          // Identifier linking to the context/key used for encryption.
        // This ID is resolved by the KeyManagementService to find the actual key
        // (e.g., it could be a UserKey ID or a ShareGrant ID).
        // Optional: byte[] nonceOrIv - Some algorithms (like AES/GCM) bundle this, others require separate storage.
        // For simplicity initially, assume it's bundled or handled within EncryptionService implementation if needed.

) implements ValueObject { // Implements ValueObject as it's defined by its attributes

    /**
     * Canonical constructor validating inputs.
     */
    public EncryptedValue {
        Objects.requireNonNull(encryptedData, "encryptedData cannot be null");
        Objects.requireNonNull(algorithmId, "algorithmId cannot be null");
        Objects.requireNonNull(dekContextId, "dekContextId cannot be null");
        // Defensive copy for byte array immutability
        encryptedData = Arrays.copyOf(encryptedData, encryptedData.length);
    }

    /**
     * Override equals to perform content-based comparison for the byte array.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptedValue<?> that = (EncryptedValue<?>) o;
        return Arrays.equals(encryptedData, that.encryptedData) &&
                algorithmId.equals(that.algorithmId) &&
                dekContextId.equals(that.dekContextId);
    }

    /**
     * Override hashCode to be consistent with the content-based equals.
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(algorithmId, dekContextId);
        result = 31 * result + Arrays.hashCode(encryptedData);
        return result;
    }

    /**
     * Override toString for security (avoid logging raw encrypted data).
     */
    @Override
    public String toString() {
        return "EncryptedValue{" +
                "algorithmId='" + algorithmId + '\'' +
                ", dekContextId=" + dekContextId +
                ", encryptedData=[PROTECTED]" + // Avoid exposing raw bytes
                '}';
    }

    /**
     * Provides access to a defensive copy of the encrypted data.
     * @return A copy of the encrypted byte array.
     */
    public byte[] getEncryptedData() {
        return Arrays.copyOf(encryptedData, encryptedData.length);
    }
}
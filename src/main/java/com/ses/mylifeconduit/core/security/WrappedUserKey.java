package com.ses.mylifeconduit.core.security; // Note: Package remains core.security

import com.ses.mylifeconduit.core.ddd.ValueObject;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the result of generating and wrapping a user's data encryption key.
 * Contains the wrapped key bytes and metadata about the wrapping process.
 * (Renamed from WrappedKeyInfo).
 *
 * @param wrappedKey  The raw bytes of the user's data key, encrypted (wrapped) by a Tenant Master Key.
 * @param masterKeyId Identifier of the Tenant Master Key used for wrapping.
 * @param algorithmId Algorithm used to wrap the key (might be related to the master key type).
 */
public record WrappedUserKey( // <<< RENAMED HERE
                              byte[] wrappedKey,
                              String masterKeyId,
                              String algorithmId
) implements ValueObject {

    /**
     * Canonical constructor with validation and defensive copying.
     */
    public WrappedUserKey { // <<< RENAMED HERE
        Objects.requireNonNull(wrappedKey, "wrappedKey cannot be null");
        Objects.requireNonNull(masterKeyId, "masterKeyId cannot be null or empty");
        Objects.requireNonNull(algorithmId, "algorithmId cannot be null or empty");
        if (masterKeyId.isBlank()) throw new IllegalArgumentException("masterKeyId cannot be blank");
        if (algorithmId.isBlank()) throw new IllegalArgumentException("algorithmId cannot be blank");

        // Defensive copy for immutability
        wrappedKey = Arrays.copyOf(wrappedKey, wrappedKey.length);
    }

    /**
     * Returns a defensive copy of the wrapped key bytes.
     *
     * @return A copy of the wrapped key byte array.
     */
    public byte[] wrappedKey() {
        return Arrays.copyOf(wrappedKey, wrappedKey.length);
    }

    // Override equals and hashCode for proper byte array comparison

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedUserKey that = (WrappedUserKey) o; // <<< RENAMED HERE
        return Arrays.equals(wrappedKey, that.wrappedKey) &&
                Objects.equals(masterKeyId, that.masterKeyId) &&
                Objects.equals(algorithmId, that.algorithmId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(masterKeyId, algorithmId);
        result = 31 * result + Arrays.hashCode(wrappedKey);
        return result;
    }

    /**
     * Override toString for security (avoid logging raw key bytes).
     */
    @Override
    public String toString() {
        return "WrappedUserKey{" + // <<< RENAMED HERE
                "wrappedKey=[PROTECTED]" +
                ", masterKeyId='" + masterKeyId + '\'' +
                ", algorithmId='" + algorithmId + '\'' +
                '}';
    }
}
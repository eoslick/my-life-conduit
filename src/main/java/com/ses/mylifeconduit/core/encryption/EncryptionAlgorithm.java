// src/main/java/com/ses/mylifeconduit/core/encryption/EncryptionAlgorithm.java
package com.ses.mylifeconduit.core.encryption;

/**
 * Defines standard identifiers for supported encryption algorithms.
 * These IDs are stored within {@link EncryptedValue} metadata.
 */
public final class EncryptionAlgorithm {

    private EncryptionAlgorithm() {
        // Prevent instantiation
    }

    /**
     * Identifier for AES/GCM/NoPadding with a 256-bit key. Strong default choice.
     */
    public static final String AES_GCM_256 = "AES_GCM_256";

    /**
     * Identifier for a "No Operation" encryption service, primarily for testing.
     * WARNING: Provides no actual encryption. Should NOT be used in production.
     */
    public static final String NOOP = "NOOP";

    // Add other algorithm identifiers here if needed in the future
    // public static final String AES_CBC_PKCS5_256 = "AES_CBC_PKCS5_256";

}
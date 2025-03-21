package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.ddd.domain.Entity;

/**
 * Metadata about an encryption algorithm used in the system.
 * Contains information necessary to identify and describe the algorithm
 * without exposing implementation details.
 */
public class EncryptionMetadata extends Entity<EncryptionMetadataId> {
    
    private final String algorithmName;
    private final String version;
    private final String description;
    private final int keySizeInBits;
    private final boolean supportsFutureProofing;
    
    /**
     * Creates a new encryption metadata entry.
     * 
     * @param id The unique identifier for this metadata
     * @param algorithmName The name of the encryption algorithm (e.g., "AES-GCM")
     * @param version The version of the algorithm implementation
     * @param description A human-readable description of the algorithm
     * @param keySizeInBits The key size in bits used by this algorithm
     * @param supportsFutureProofing Whether this algorithm supports future-proofing features
     */
    public EncryptionMetadata(
            EncryptionMetadataId id,
            String algorithmName,
            String version,
            String description,
            int keySizeInBits,
            boolean supportsFutureProofing) {
        super(id);
        this.algorithmName = algorithmName;
        this.version = version;
        this.description = description;
        this.keySizeInBits = keySizeInBits;
        this.supportsFutureProofing = supportsFutureProofing;
    }
    
    /**
     * Gets the name of the encryption algorithm.
     * 
     * @return The algorithm name
     */
    public String getAlgorithmName() {
        return algorithmName;
    }
    
    /**
     * Gets the version of the algorithm implementation.
     * 
     * @return The version identifier
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Gets a human-readable description of the algorithm.
     * 
     * @return The algorithm description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the key size in bits used by this algorithm.
     * 
     * @return The key size in bits
     */
    public int getKeySizeInBits() {
        return keySizeInBits;
    }
    
    /**
     * Checks if this algorithm supports future-proofing features.
     * 
     * @return True if future-proofing is supported, false otherwise
     */
    public boolean supportsFutureProofing() {
        return supportsFutureProofing;
    }
}
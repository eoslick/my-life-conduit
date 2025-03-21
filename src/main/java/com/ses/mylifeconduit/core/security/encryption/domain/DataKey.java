package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.ddd.domain.Entity;

/**
 * Represents a data encryption key in the envelope encryption model.
 * Each user has a unique data key which is wrapped (encrypted) by a tenant master key.
 */
public class DataKey extends Entity<EncryptionKeyId> {
    private final byte[] wrappedKey;
    private final EncryptionKeyId masterKeyId;
    private final EncryptionMetadataId encryptionMetadataId;
    
    /**
     * Creates a new data key.
     *
     * @param id The unique identifier for this key
     * @param wrappedKey The encrypted key material
     * @param masterKeyId The ID of the master key that wrapped this key
     * @param encryptionMetadataId The ID of the encryption algorithm used to wrap this key
     */
    public DataKey(
            EncryptionKeyId id,
            byte[] wrappedKey,
            EncryptionKeyId masterKeyId,
            EncryptionMetadataId encryptionMetadataId) {
        super(id);
        this.wrappedKey = wrappedKey;
        this.masterKeyId = masterKeyId;
        this.encryptionMetadataId = encryptionMetadataId;
    }
    
    /**
     * Gets the wrapped (encrypted) key material.
     *
     * @return The wrapped key
     */
    public byte[] getWrappedKey() {
        return wrappedKey;
    }
    
    /**
     * Gets the ID of the master key used to wrap this data key.
     *
     * @return The master key ID
     */
    public EncryptionKeyId getMasterKeyId() {
        return masterKeyId;
    }
    
    /**
     * Gets the encryption metadata ID for the algorithm used to wrap this key.
     *
     * @return The encryption metadata ID
     */
    public EncryptionMetadataId getEncryptionMetadataId() {
        return encryptionMetadataId;
    }
}
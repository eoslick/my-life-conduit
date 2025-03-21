package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.ddd.domain.EntityId;

import java.util.UUID;

/**
 * Strongly-typed identifier for encryption keys.
 * Each user's data key and each tenant's master key has a unique ID.
 */
public class EncryptionKeyId extends EntityId {
    
    /**
     * Creates a new encryption key identifier with a random UUID.
     */
    public EncryptionKeyId() {
        super(UUID.randomUUID());
    }
    
    /**
     * Creates an encryption key identifier with a specific UUID.
     * 
     * @param id The UUID to use for this identifier
     */
    public EncryptionKeyId(UUID id) {
        super(id);
    }
}
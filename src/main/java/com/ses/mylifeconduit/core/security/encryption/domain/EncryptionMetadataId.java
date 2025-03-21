package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.ddd.domain.EntityId;

import java.util.UUID;

/**
 * Strongly-typed identifier for encryption metadata.
 * Used to reference specific encryption algorithms and their metadata.
 */
public class EncryptionMetadataId extends EntityId {
    
    /**
     * Creates a new encryption metadata identifier with a random UUID.
     */
    public EncryptionMetadataId() {
        super(UUID.randomUUID());
    }
    
    /**
     * Creates an encryption metadata identifier with a specific UUID.
     * 
     * @param id The UUID to use for this identifier
     */
    public EncryptionMetadataId(UUID id) {
        super(id);
    }
}
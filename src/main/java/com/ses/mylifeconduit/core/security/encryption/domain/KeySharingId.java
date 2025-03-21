package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.ddd.domain.EntityId;

import java.util.UUID;

/**
 * Strongly-typed identifier for key sharing grants.
 */
public class KeySharingId extends EntityId {
    
    /**
     * Creates a new key sharing identifier with a random UUID.
     */
    public KeySharingId() {
        super(UUID.randomUUID());
    }
    
    /**
     * Creates a key sharing identifier with a specific UUID.
     * 
     * @param id The UUID to use for this identifier
     */
    public KeySharingId(UUID id) {
        super(id);
    }
}
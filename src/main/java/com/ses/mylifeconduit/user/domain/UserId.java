package com.ses.mylifeconduit.user.domain;

import com.ses.mylifeconduit.core.ddd.domain.EntityId;

import java.util.UUID;

/**
 * Strongly-typed identifier for users in the system.
 */
public class UserId extends EntityId {
    
    /**
     * Creates a new user identifier with a random UUID.
     */
    public UserId() {
        super(UUID.randomUUID());
    }
    
    /**
     * Creates a user identifier with a specific UUID.
     * 
     * @param id The UUID to use for this identifier
     */
    public UserId(UUID id) {
        super(id);
    }
}
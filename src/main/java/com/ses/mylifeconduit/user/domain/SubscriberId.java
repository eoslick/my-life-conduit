package com.ses.mylifeconduit.user.domain;

import com.ses.mylifeconduit.core.ddd.domain.AggregateId;

import java.util.UUID;

/**
 * Identifier for a Subscriber in the system.
 */
public class SubscriberId extends AggregateId {
    
    /**
     * Creates a new subscriber identifier with a random UUID.
     */
    public SubscriberId() {
        super(UUID.randomUUID());
    }
    
    /**
     * Creates a subscriber identifier with a specific UUID.
     * 
     * @param id The UUID to use for this identifier
     */
    public SubscriberId(UUID id) {
        super(id);
    }
    
    /**
     * Creates a subscriber identifier from a user ID.
     * 
     * @param userId The system user ID associated with this subscriber
     * @return The corresponding subscriber ID
     */
    public static SubscriberId fromUserId(UserId userId) {
        return new SubscriberId(userId.getValue());
    }
}
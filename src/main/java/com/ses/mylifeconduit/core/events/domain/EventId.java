package com.ses.mylifeconduit.core.events.domain;

import com.ses.mylifeconduit.core.ddd.domain.EntityId;

import java.util.UUID;

/**
 * Strongly-typed identifier for domain events.
 */
public class EventId extends EntityId {
    
    /**
     * Creates a new event identifier with a random UUID.
     */
    public EventId() {
        super(UUID.randomUUID());
    }
    
    /**
     * Creates an event identifier with a specific UUID.
     * 
     * @param id The UUID to use for this identifier
     */
    public EventId(UUID id) {
        super(id);
    }
}
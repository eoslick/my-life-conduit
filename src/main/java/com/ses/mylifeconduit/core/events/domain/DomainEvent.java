package com.ses.mylifeconduit.core.events.domain;

import com.ses.mylifeconduit.core.ddd.domain.AggregateId;

import java.time.Instant;

/**
 * Base interface for all domain events in the system.
 * Domain events represent something significant that happened in the domain.
 * They are immutable and represent a fact that occurred at a specific point in time.
 */
public interface DomainEvent {
    
    /**
     * Gets the unique identifier for this event.
     * 
     * @return The event ID
     */
    EventId getEventId();
    
    /**
     * Gets the timestamp when this event occurred.
     * 
     * @return The event timestamp
     */
    Instant getOccurredAt();
    
    /**
     * Gets the identifier of the aggregate this event belongs to.
     * Events are always associated with an aggregate, which is the
     * consistency boundary in domain-driven design.
     * 
     * @return The aggregate ID
     */
    AggregateId getAggregateId();
    
    /**
     * Gets the type of this event.
     * This is typically used for event serialization and deserialization.
     * 
     * @return The event type
     */
    String getEventType();
    
    /**
     * Gets the version of this event.
     * This helps with event evolution over time.
     * 
     * @return The event version
     */
    int getEventVersion();
}
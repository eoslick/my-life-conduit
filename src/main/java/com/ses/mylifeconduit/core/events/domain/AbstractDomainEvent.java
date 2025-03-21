package com.ses.mylifeconduit.core.events.domain;

import com.ses.mylifeconduit.core.ddd.domain.AggregateId;

import java.time.Instant;

/**
 * Abstract base class for domain events that provides common functionality.
 * This class implements the DomainEvent interface and handles the standard
 * event properties like ID, timestamp, and type.
 */
public abstract class AbstractDomainEvent implements DomainEvent {
    private final EventId eventId;
    private final Instant occurredAt;
    private final AggregateId aggregateId;
    private final String eventType;
    private final int eventVersion;
    
    /**
     * Creates a new domain event.
     *
     * @param eventId The unique identifier for this event
     * @param occurredAt When the event occurred
     * @param aggregateId The identifier of the aggregate this event belongs to
     * @param eventVersion The version of this event
     */
    protected AbstractDomainEvent(
            EventId eventId,
            Instant occurredAt,
            AggregateId aggregateId,
            int eventVersion) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.aggregateId = aggregateId;
        this.eventType = deriveEventType();
        this.eventVersion = eventVersion;
    }
    
    /**
     * Creates a new domain event with the current timestamp.
     *
     * @param aggregateId The identifier of the aggregate this event belongs to
     */
    protected AbstractDomainEvent(AggregateId aggregateId) {
        this(new EventId(), Instant.now(), aggregateId, 1);
    }
    
    @Override
    public EventId getEventId() {
        return eventId;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public AggregateId getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public String getEventType() {
        return eventType;
    }
    
    @Override
    public int getEventVersion() {
        return eventVersion;
    }
    
    /**
     * Derives the event type from the class name.
     *
     * @return The event type string
     */
    private String deriveEventType() {
        return this.getClass().getSimpleName();
    }
}
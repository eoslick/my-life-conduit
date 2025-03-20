package com.ses.mylifeconduit.core.ddd.domain;

import com.ses.mylifeconduit.core.ddd.exceptions.InvariantViolationException;
import com.ses.mylifeconduit.core.events.domain.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all aggregates, providing event sourcing functionality.
 */
public abstract class Aggregate {
    private final AggregateId id;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    /**
     * Constructs an aggregate with a unique ID.
     * @param id The aggregate's unique identifier
     */
    protected Aggregate(AggregateId id) {
        this.id = id;
    }

    /**
     * Applies an event to update the aggregate's state.
     * @param event The domain event to apply
     * @throws InvariantViolationException if the event violates aggregate invariants
     */
    protected void applyEvent(DomainEvent event) throws InvariantViolationException {
        updateState(event);
        uncommittedEvents.add(event);
    }

    /**
     * Updates the aggregate's state based on the event.
     * Subclasses must implement this to define state changes.
     * @param event The domain event to process
     * @throws InvariantViolationException if invariants are violated
     */
    protected abstract void updateState(DomainEvent event) throws InvariantViolationException;

    public AggregateId getId() {
        return id;
    }

    public List<DomainEvent> getUncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }
}
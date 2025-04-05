// src/main/java/com/ses/mylifeconduit/core/eventsourcing/exception/ConcurrencyException.java
package com.ses.mylifeconduit.core.eventsourcing.exception;

import com.ses.mylifeconduit.core.ddd.EntityId;

/**
 * Exception thrown when an optimistic concurrency conflict is detected while appending events.
 * This typically means another process has modified the aggregate since it was loaded.
 */
public class ConcurrencyException extends EventStoreException {

    private final EntityId aggregateId;
    private final long expectedVersion;
    private final long actualVersion; // Optional: Might not always be known

    public ConcurrencyException(String message, EntityId aggregateId, long expectedVersion) {
        super(message);
        this.aggregateId = aggregateId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = -1; // Indicate actual version wasn't provided
    }

    public ConcurrencyException(String message, Throwable cause, EntityId aggregateId, long expectedVersion) {
        super(message, cause);
        this.aggregateId = aggregateId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = -1;
    }

    public ConcurrencyException(String message, EntityId aggregateId, long expectedVersion, long actualVersion) {
        super(message);
        this.aggregateId = aggregateId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public EntityId getAggregateId() {
        return aggregateId;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public long getActualVersion() {
        return actualVersion;
    }
}
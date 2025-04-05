// src/main/java/com/ses/mylifeconduit/core/eventsourcing/exception/EventStoreException.java
package com.ses.mylifeconduit.core.eventsourcing.exception;

/**
 * Base exception for errors related to the Event Store operations (e.g., persistence, loading).
 */
public class EventStoreException extends RuntimeException {

    public EventStoreException(String message) {
        super(message);
    }

    public EventStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
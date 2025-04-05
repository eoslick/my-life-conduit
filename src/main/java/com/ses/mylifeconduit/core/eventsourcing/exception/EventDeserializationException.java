// src/main/java/com/ses/mylifeconduit/core/eventsourcing/exception/EventDeserializationException.java
package com.ses.mylifeconduit.core.eventsourcing.exception;

/**
 * Exception thrown when an event payload cannot be deserialized from its stored format
 * back into a DomainEvent object.
 */
public class EventDeserializationException extends EventStoreException {

    public EventDeserializationException(String message) {
        super(message);
    }

    public EventDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
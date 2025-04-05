package com.ses.mylifeconduit.core.ddd;

import com.ses.mylifeconduit.core.tenant.TenantId;
// No longer need to import specific event types here

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a significant occurrence in the domain that domain experts care about.
 * Domain Events are immutable facts about things that have happened in the past.
 * <p>
 * NOTE: This interface is NOT sealed to allow implementations in different packages
 * corresponding to their bounded contexts, especially when not using explicit Java Modules.
 */
// <<< REMOVED 'sealed' and 'permits' clause >>>
public interface DomainEvent extends Serializable {

    /**
     * Unique identifier for the specific event occurrence.
     */
    UUID eventId();

    /**
     * Identifier of the aggregate instance this event pertains to.
     */
    EntityId aggregateId();

    /**
     * The tenant context in which the event occurred.
     */
    TenantId tenantId();

    /**
     * Timestamp indicating when the event occurred in the domain.
     */
    Instant occurredOn();

    /**
     * The version of the aggregate *after* this event was applied.
     */
    long aggregateVersion();

    /**
     * The schema version of this event payload (e.g., "V1", "V2").
     * Used for handling event evolution.
     */
    String eventVersion();
}
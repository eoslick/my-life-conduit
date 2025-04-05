// --- File: src/main/java/com/ses/mylifeconduit/core/eventsourcing/EventStore.java ---
package com.ses.mylifeconduit.core.eventsourcing;

import com.ses.mylifeconduit.core.ddd.DomainEvent;
import com.ses.mylifeconduit.core.ddd.EntityId;
import com.ses.mylifeconduit.core.eventsourcing.exception.ConcurrencyException;
import com.ses.mylifeconduit.core.eventsourcing.exception.EventStoreException;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId; // Needed for metadata

import java.util.List;
// No longer need java.util.UUID here for appendEvents signature

/**
 * Interface for persisting and retrieving streams of domain events.
 * This forms the persistence backbone for event-sourced aggregates.
 * <p>
 * Implementations will typically interact with a database and collaborate with the
 * {@link com.ses.mylifeconduit.core.encryption.EncryptionService}
 * to handle payload encryption/decryption.
 */
public interface EventStore {

    /**
     * Appends a list of domain events for a specific aggregate instance to the store.
     * <p>
     * This operation MUST be atomic and perform an optimistic concurrency check based on
     * the expected version of the aggregate. The necessary KeyContext (containing DekId)
     * for encryption will be generated internally by the implementation for each event.
     *
     * @param aggregateId     The ID of the aggregate the events belong to.
     * @param aggregateType   The type name of the aggregate (for potential indexing/querying).
     * @param expectedVersion The version the aggregate is expected to be at before these events.
     *                        Used for optimistic concurrency check. Pass 0 if creating a new aggregate.
     * @param events          The list of {@link DomainEvent}s to append. Must not be empty.
     * @param tenantId        The tenant context.
     * @param userId          The user context causing the events (for audit metadata).
     * // <<< REMOVED UUID keyContextId PARAMETER >>>
     * @throws IllegalArgumentException if events list is null or empty, or other IDs are null.
     * @throws ConcurrencyException     if the actual current version of the aggregate in the store
     *                                  does not match the {@code expectedVersion}.
     * @throws EventStoreException      if any other error occurs during persistence, including
     *                                  serialization or encryption failures within the implementation.
     */
    void appendEvents(
            EntityId aggregateId,
            String aggregateType,
            long expectedVersion,
            List<? extends DomainEvent> events,
            TenantId tenantId,
            UserId userId
            // <<< REMOVED UUID keyContextId PARAMETER >>>
    );

    /**
     * Loads the full stream of stored events for a specific aggregate instance.
     *
     * @param aggregateId The ID of the aggregate whose event stream is to be loaded.
     * @param tenantId    The tenant context.
     * @return A list of {@link StoredEvent} records in the order they were appended.
     *         Returns an empty list if the aggregate does not exist.
     * @throws IllegalArgumentException if IDs are null.
     * @throws EventStoreException      if any error occurs during retrieval.
     */
    List<StoredEvent> loadEventStream(EntityId aggregateId, TenantId tenantId);

    /**
     * Loads the stream of stored events for a specific aggregate instance, starting
     * from a particular version (sequence number). Useful for snapshots or partial replays.
     *
     * @param aggregateId    The ID of the aggregate.
     * @param tenantId       The tenant context.
     * @param afterVersion   The sequence number (exclusive) after which to load events.
     * @return A list of {@link StoredEvent} records occurring after the specified version.
     * @throws IllegalArgumentException if IDs are null or afterVersion is negative.
     * @throws EventStoreException      if any error occurs during retrieval.
     */
    List<StoredEvent> loadEventStreamAfter(EntityId aggregateId, TenantId tenantId, long afterVersion);

    // Optional: Methods for querying events across aggregates
    // List<StoredEvent> findEventsByType(String eventType, TenantId tenantId, Instant since);
}
// --- End File: src/main/java/com/ses/mylifeconduit/core/eventsourcing/EventStore.java ---
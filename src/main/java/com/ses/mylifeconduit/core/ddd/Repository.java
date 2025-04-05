// src/main/java/com/ses/mylifeconduit/core/ddd/Repository.java
package com.ses.mylifeconduit.core.ddd;

import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;

import java.util.Optional;

/**
 * Generic interface for a Repository in Domain-Driven Design.
 * <p>
 * Repositories abstract the persistence mechanism for Aggregates, providing an
 * interface that looks like an in-memory collection of Aggregate Roots.
 * <p>
 * Implementations typically interact with an Event Store in an Event Sourcing context.
 *
 * @param <T>  The type of the Aggregate Root managed by this repository.
 * @param <ID> The type of the Aggregate Root's identifier.
 */
public interface Repository<T extends AggregateRoot<ID>, ID extends EntityId> {

    /**
     * Saves an aggregate instance.
     * <p>
     * In an event sourcing context, this typically involves:
     * 1. Retrieving uncommitted changes (events) from the aggregate.
     * 2. Appending these events to the event store, handling optimistic concurrency checks.
     * 3. Marking the changes as committed on the aggregate instance.
     * 4. Optionally publishing the events to an event bus.
     *
     * @param aggregate The aggregate instance to save. Must not be null.
     * @param tenantId  The tenant context for persistence. Must not be null.
     * @throws NullPointerException if aggregate or tenantId is null.
     * @throws com.ses.mylifeconduit.core.eventsourcing.ConcurrencyException if a conflicting version is detected.
     * @throws com.ses.mylifeconduit.core.eventsourcing.EventStoreException on other persistence errors.
     */
    void save(T aggregate, TenantId tenantId, UserId actingUserId);

    /**
     * Finds and reconstitutes an aggregate instance by its identifier.
     * <p>
     * In an event sourcing context, this typically involves:
     * 1. Loading the full stream of events for the given aggregate ID from the event store.
     * 2. Creating a new, empty instance of the aggregate root.
     * 3. Applying the loaded event history to the instance to restore its state.
     *
     * @param id       The unique identifier of the aggregate to find. Must not be null.
     * @param tenantId The tenant context for retrieval. Must not be null.
     * @return An {@link Optional} containing the reconstituted aggregate if found,
     *         or {@link Optional#empty()} if no aggregate exists with the given ID.
     * @throws NullPointerException if id or tenantId is null.
     * @throws com.ses.mylifeconduit.core.eventsourcing.EventStoreException on persistence errors during loading.
     * @throws RuntimeException on errors during event application/replay.
     */
    Optional<T> findById(ID id, TenantId tenantId);

    // Other common repository methods like delete, findByCriteria etc. could be added
    // but are often less common or implemented differently in pure Event Sourcing.
    // Deletion might be represented by a "Deleted" event.
    // Querying is often handled by separate Read Models/Projections.

}
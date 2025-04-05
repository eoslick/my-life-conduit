package com.ses.mylifeconduit.goalsetting.domain;

import com.ses.mylifeconduit.core.ddd.Repository; // Import base interface
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId; // Import Aggregate ID type

import java.util.Optional;

/**
 * Repository interface for managing the persistence of UserGoals aggregates.
 *
 * Extends the generic DDD Repository, specifying UserGoals as the Aggregate Root
 * and UserId as its identifier type.
 */
public interface UserGoalsRepository extends Repository<UserGoals, UserId> {

    /**
     * Saves a UserGoals aggregate instance, recording the user who initiated the change.
     *
     * @param aggregate     The UserGoals aggregate to save.
     * @param tenantId      The tenant context.
     * @param actingUserId  The ID of the user performing the action causing the save.
     * @throws com.ses.mylifeconduit.core.eventsourcing.exception.ConcurrencyException If optimistic locking fails.
     * @throws com.ses.mylifeconduit.core.eventsourcing.exception.EventStoreException On other persistence errors.
     */
    @Override
    void save(UserGoals aggregate, TenantId tenantId, UserId actingUserId);

    /**
     * Finds and reconstitutes a UserGoals aggregate instance by its UserId.
     *
     * @param id       The UserId of the aggregate to find.
     * @param tenantId The tenant context.
     * @return An {@link Optional} containing the reconstituted aggregate if found,
     *         or {@link Optional#empty()} otherwise.
     * @throws com.ses.mylifeconduit.core.eventsourcing.exception.EventStoreException On persistence errors during loading or reconstitution failures.
     */
    @Override
    Optional<UserGoals> findById(UserId id, TenantId tenantId);

    // No additional context-specific methods needed for basic ES repository.
    // Queries for goals matching criteria would typically use separate read models.
}
// --- File: com/ses/mylifeconduit/corevalues/domain/UserCoreValuesRepository.java ---
package com.ses.mylifeconduit.corevalues.domain;

import com.ses.mylifeconduit.core.ddd.Repository;
import com.ses.mylifeconduit.core.user.UserId;
import com.ses.mylifeconduit.core.tenant.TenantId;

import java.util.Optional;

/**
 * Repository interface for managing the persistence of UserCoreValues aggregates.
 */
public interface UserCoreValuesRepository extends Repository<UserCoreValues, UserId> {

    /**
     * Saves a UserCoreValues aggregate instance.
     * (Inherits the updated signature with actingUserId from the base Repository interface)
     *
     * @param aggregate     The UserCoreValues aggregate to save.
     * @param tenantId      The tenant context.
     * @throws com.ses.mylifeconduit.core.eventsourcing.exception.ConcurrencyException If optimistic locking fails.
     * @throws com.ses.mylifeconduit.core.eventsourcing.exception.EventStoreException On other persistence errors.
     */
    @Override
    void save(UserCoreValues aggregate, TenantId tenantId, UserId actingUserId); // <<< Signature updated

    /**
     * Finds and reconstitutes a UserCoreValues aggregate instance by its UserId.
     * (Signature remains unchanged)
     */
    @Override
    Optional<UserCoreValues> findById(UserId id, TenantId tenantId);

}
// --- End File ---
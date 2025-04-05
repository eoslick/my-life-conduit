// --- File: src/main/java/com/ses/mylifeconduit/core/security/keys/KeyRepository.java ---
package com.ses.mylifeconduit.core.security.keys;

import com.ses.mylifeconduit.core.security.sharing.ShareGrantDetails;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;

import java.time.Instant;
import java.util.*;

/**
 * Interface for persisting and retrieving cryptographic key material and sharing grants.
 */
public interface KeyRepository {

    // --- User Key Methods ---
    /**
     * Saves or updates the wrapped primary data key for a user.
     *
     * @param tenantId    Tenant context.
     * @param userId      User identifier.
     * @param wrappedKey  The wrapped key bytes.
     * @param masterKeyId Identifier of the master key used for wrapping.
     * @param algorithmId Algorithm used for wrapping.
     */
    void saveWrappedUserKey(TenantId tenantId, UserId userId, byte[] wrappedKey, String masterKeyId, String algorithmId);

    /**
     * Finds the stored wrapped primary data key for a user.
     *
     * @param tenantId Tenant context.
     * @param userId   User identifier.
     * @return Optional containing the {@link StoredUserKey} if found.
     */
    Optional<StoredUserKey> findWrappedUserKey(TenantId tenantId, UserId userId);

    // --- Wrapped DEK Methods ---
    /**
     * Saves the owner's wrapped Data Encryption Key (DEK).
     *
     * @param dek The {@link StoredWrappedDek} object containing the DekId, owner info,
     *            wrapped key, and metadata. <<< PARAMETER TYPE UPDATED (implicitly by using StoredWrappedDek)
     */
    void saveWrappedDek(StoredWrappedDek dek);

    /**
     * Finds the owner's wrapped DEK by its unique ID.
     * Note: Access control (checking if the caller *should* see this) is typically handled
     * in the KeyManagementService layer, not the repository.
     *
     * @param dekId    The unique ID of the DEK to find. <<< CHANGED PARAMETER TYPE
     * @param tenantId The tenant context.
     * @return Optional containing the {@link StoredWrappedDek} if found.
     */
    Optional<StoredWrappedDek> findWrappedDekById(DekId dekId, TenantId tenantId); // <<< RENAMED METHOD & CHANGED PARAM TYPE


    // --- Share Grant Methods ---
    /**
     * Persists the details of a data sharing grant.
     * The {@code details.dataReference()} field MUST contain the string representation
     * of the {@link DekId} being shared.
     *
     * @param tenantId             Tenant context.
     * @param grantId              The unique ID for this grant.
     * @param details              The details of the grant (owner, grantee, dekId reference, expiration).
     * @param wrappedDekForGrantee The original DEK, re-wrapped/encrypted for the grantee.
     * @param granteeKeyId         Identifier of the grantee's key used for re-wrapping.
     * @param algorithmId          Algorithm used for re-wrapping.
     */
    void saveShareGrant(TenantId tenantId, ShareGrantId grantId, ShareGrantDetails details, byte[] wrappedDekForGrantee, String granteeKeyId, String algorithmId);

    /**
     * Retrieves the details of a specific share grant by its ID.
     *
     * @param tenantId Tenant context.
     * @param grantId  The unique ID of the grant.
     * @return Optional containing the {@link StoredShareGrant} if found.
     */
    Optional<StoredShareGrant> findShareGrantById(TenantId tenantId, ShareGrantId grantId);

    /**
     * Finds active (non-expired) share grants that provide the specified user access
     * to the specified Data Encryption Key (DEK).
     *
     * @param tenantId        Tenant context.
     * @param dekId           The {@link DekId} of the key being accessed. <<< CHANGED PARAMETER TYPE
     * @param accessingUserId The user attempting access.
     * @return A list of {@link StoredShareGrant} objects matching the criteria. May be empty.
     */
    List<StoredShareGrant> findActiveShareGrantsForDek(TenantId tenantId, DekId dekId, UserId accessingUserId); // <<< RENAMED METHOD & CHANGED PARAM TYPE


    /**
     * Deletes a share grant by its ID.
     *
     * @param tenantId Tenant context.
     * @param grantId  The unique ID of the grant to delete.
     */
    void deleteShareGrant(TenantId tenantId, ShareGrantId grantId);

    /**
     * Finds share grants that have expired based on a cutoff timestamp.
     * Useful for cleanup tasks.
     *
     * @param cutoffTimestamp Grants with expiration before this time are considered expired.
     * @return A list of expired {@link StoredShareGrant} objects.
     */
    List<StoredShareGrant> findExpiredShareGrants(Instant cutoffTimestamp);

}
// --- End File: src/main/java/com/ses/mylifeconduit/core/security/keys/KeyRepository.java ---
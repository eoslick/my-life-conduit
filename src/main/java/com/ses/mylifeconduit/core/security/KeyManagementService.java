// --- File: src/main/java/com/ses/mylifeconduit/core/security/KeyManagementService.java ---
package com.ses.mylifeconduit.core.security; // Assuming this is the correct package

import com.ses.mylifeconduit.core.security.keys.DekId; // <<< Import DekId
import com.ses.mylifeconduit.core.security.keys.ShareGrantId;
import com.ses.mylifeconduit.core.security.sharing.ShareGrantDetails;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;

import javax.crypto.SecretKey;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing cryptographic keys, including user keys,
 * data encryption keys (DEKs), and sharing grants.
 */
public interface KeyManagementService {

    /**
     * Retrieves and unwraps the primary data key for a given user.
     *
     * @param tenantId The tenant context.
     * @param userId   The user whose key is needed.
     * @return The unwrapped {@link SecretKey}.
     * @throws com.ses.mylifeconduit.core.security.exception.KeyManagementException if the key is not found or unwrapping fails.
     */
    SecretKey unwrapUserKey(TenantId tenantId, UserId userId);

    /**
     * Generates a new primary data key for a user, wraps it using the tenant's master key,
     * and persists the wrapped key.
     *
     * @param tenantId The tenant context.
     * @param userId   The user for whom to generate the key.
     * @return Information about the wrapped key ({@link WrappedUserKey}).
     * @throws com.ses.mylifeconduit.core.security.exception.KeyManagementException if generation, wrapping, or saving fails.
     */
    WrappedUserKey generateAndWrapUserKey(TenantId tenantId, UserId userId);

    /**
     * Creates a share grant, allowing a grantee to access data originally encrypted
     * with a specific DEK. This involves re-encrypting the DEK for the grantee.
     * <p>
     * IMPORTANT: The {@link ShareGrantDetails#dataReference()} field is expected to contain
     * the string representation of the {@link DekId} being shared.
     *
     * @param tenantId The tenant context.
     * @param details  Details including owner, grantee, and the DEK ID (within dataReference).
     * @return The unique {@link ShareGrantId} for the created grant.
     * @throws com.ses.mylifeconduit.core.security.exception.KeyManagementException if key resolution or persistence fails.
     * @throws IllegalArgumentException if details.dataReference is not a valid DekId representation.
     */
    ShareGrantId createShareGrant(TenantId tenantId, ShareGrantDetails details);

    /**
     * Revokes a specific share grant by its ID.
     *
     * @param tenantId       The tenant context.
     * @param shareGrantId   The ID of the grant to revoke.
     * //@param requestingUserId The user attempting the revocation (for authorization checks - removed for simplicity for now).
     * @throws com.ses.mylifeconduit.core.security.exception.KeyManagementException if deletion fails.
     * @throws com.ses.mylifeconduit.core.security.exception.AccessDeniedException if the requesting user is not authorized.
     */
    void revokeShareGrant(TenantId tenantId, ShareGrantId shareGrantId /*, UserId requestingUserId */);


    /**
     * Resolves the appropriate Data Encryption Key (DEK) for decrypting data associated
     * with a given context identifier.
     * <p>
     * The service first checks if the {@code retrievalContextId} corresponds to an active
     * {@link ShareGrantId} for the {@code accessingUserId}. If found, it unwraps the DEK
     * shared via the grant.
     * <p>
     * If no valid grant is found, it assumes the {@code retrievalContextId} represents a
     * {@link DekId} and attempts to retrieve the DEK directly for the owner (assuming
     * the {@code accessingUserId} is the owner).
     *
     * @param tenantId         Tenant context.
     * @param accessingUserId  User attempting access.
     * @param retrievalContextId The context ID associated with the encrypted data (from EncryptedValue),
     *                           which could be a ShareGrantId or a DekId. <<< RENAMED PARAMETER
     * @return {@code Optional<SecretKey>} containing the plaintext DEK if authorized and resolved,
     *         otherwise {@link Optional#empty()}.
     * @throws com.ses.mylifeconduit.core.security.exception.KeyManagementException on underlying key resolution errors.
     */
    Optional<SecretKey> resolveDecryptionKey(TenantId tenantId, UserId accessingUserId, UUID retrievalContextId); // <<< RENAMED PARAMETER


    /**
     * Generates a new Data Encryption Key (DEK), wraps it using the owner's primary user key,
     * and persists the wrapped DEK information.
     *
     * @param tenantId    Tenant context.
     * @param ownerUserId User who will own this DEK.
     * @param dek         Plaintext DEK to wrap and store.
     * @return The unique {@link DekId} generated and assigned to this wrapped DEK instance. <<< CHANGED RETURN TYPE
     * @throws com.ses.mylifeconduit.core.security.exception.KeyManagementException if wrapping or persistence fails.
     */
    DekId wrapAndStoreDekForOwner(TenantId tenantId, UserId ownerUserId, SecretKey dek); // <<< CHANGED RETURN TYPE from UUID
}
// --- End File: src/main/java/com/ses/mylifeconduit/core/security/KeyManagementService.java ---
package com.ses.mylifeconduit.user.domain;

import com.ses.mylifeconduit.core.security.encryption.domain.EncryptionKeyId;
import com.ses.mylifeconduit.core.security.encryption.domain.KeySharingId;

import java.time.Duration;

/**
 * Service for managing subscriber authorizations to share encrypted data.
 * 
 * This service belongs to the User domain and focuses on the business aspects
 * of data sharing authorizations. It coordinates with the Encryption domain
 * to implement the technical aspects of key sharing.
 * 
 * Domain Boundary:
 * - User domain: Authorization policies, sharing purposes, user experience
 * - Encryption domain: Key management, cryptographic operations
 */
public interface DataSharingService {
    
    /**
     * Authorizes sharing of data with another user.
     *
     * @param subscriberId The subscriber authorizing the sharing
     * @param dataKeyId The ID of the encryption key for the data
     * @param targetUserId The user to share with
     * @param dataName A user-friendly name for the shared data
     * @param purpose The business purpose for sharing
     * @param duration How long the sharing should last (null for permanent)
     * @return The data sharing authorization
     */
    DataSharingAuthorization authorizeUserSharing(
            SubscriberId subscriberId,
            EncryptionKeyId dataKeyId,
            UserId targetUserId,
            String dataName,
            DataSharingPurpose purpose,
            Duration duration);
    
    /**
     * Authorizes sharing of data with a role.
     *
     * @param subscriberId The subscriber authorizing the sharing
     * @param dataKeyId The ID of the encryption key for the data
     * @param roleName The role to share with
     * @param dataName A user-friendly name for the shared data
     * @param purpose The business purpose for sharing
     * @param duration How long the sharing should last (null for permanent)
     * @return The data sharing authorization
     */
    DataSharingAuthorization authorizeRoleSharing(
            SubscriberId subscriberId,
            EncryptionKeyId dataKeyId,
            String roleName,
            String dataName,
            DataSharingPurpose purpose,
            Duration duration);
    
    /**
     * Revokes a previously granted sharing authorization.
     *
     * @param authorizationId The ID of the authorization to revoke
     * @param subscriberId The subscriber revoking the authorization
     * @param reason The reason for revocation (optional)
     */
    void revokeAuthorization(KeySharingId authorizationId, SubscriberId subscriberId, String reason);
    
    /**
     * Lists all active sharing authorizations granted by a subscriber.
     *
     * @param subscriberId The subscriber who granted the authorizations
     * @return An iterable of active authorizations
     */
    Iterable<DataSharingAuthorization> listActiveAuthorizationsGrantedBy(SubscriberId subscriberId);
    
    /**
     * Lists all active sharing authorizations granted to a user.
     *
     * @param userId The user who received the authorizations
     * @return An iterable of active authorizations
     */
    Iterable<DataSharingAuthorization> listActiveAuthorizationsGrantedTo(UserId userId);
}
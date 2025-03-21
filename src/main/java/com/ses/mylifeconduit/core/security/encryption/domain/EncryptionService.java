package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.security.encryption.exceptions.DecryptionException;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.user.domain.UserId;

import java.time.Duration;

/**
 * Service interface for encryption and key management operations.
 * This service handles the technical aspects of encryption/decryption and key management.
 * 
 * Domain Boundary:
 * - Encryption domain: Cryptographic operations, key management
 * - User domain: Authorization policies, sharing purposes, user experience
 */
public interface EncryptionService {
    
    /**
     * Encrypts data for a specific user with the specified encryption algorithm.
     *
     * @param userId The user the data belongs to
     * @param data The plaintext data to encrypt
     * @param encryptionMetadataId The specific encryption algorithm to use
     * @return The encrypted data package
     */
    EncryptedData encrypt(UserId userId, byte[] data, EncryptionMetadataId encryptionMetadataId);
    
    /**
     * Decrypts data using the specified encryption metadata.
     * This operation will only succeed if the current user has access to the data key.
     *
     * @param encryptedData The encrypted data to decrypt
     * @param encryptionMetadataId The encryption metadata for decryption
     * @return The decrypted plaintext data
     * @throws DecryptionException If decryption fails or the user doesn't have access
     */
    byte[] decrypt(EncryptedData encryptedData, EncryptionMetadataId encryptionMetadataId) throws DecryptionException;
    
    /**
     * Creates a key sharing between a grantor and grantee.
     * This is a technical operation called by the DataSharingService in the user domain.
     *
     * @param dataKeyId The data key to share
     * @param grantorUserId The user granting access
     * @param granteeUserId The user receiving access
     * @param duration How long the sharing should last
     * @return The key sharing entity
     */
    KeySharing createUserKeySharing(
            EncryptionKeyId dataKeyId,
            UserId grantorUserId,
            UserId granteeUserId,
            Duration duration);
    
    /**
     * Creates a key sharing between a user and a role.
     * This is a technical operation called by the DataSharingService in the user domain.
     *
     * @param dataKeyId The data key to share
     * @param grantorUserId The user granting access
     * @param roleName The role receiving access
     * @param duration How long the sharing should last
     * @return The key sharing entity
     */
    KeySharing createRoleKeySharing(
            EncryptionKeyId dataKeyId,
            UserId grantorUserId,
            String roleName,
            Duration duration);
    
    /**
     * Revokes a key sharing.
     * This is a technical operation called by the DataSharingService in the user domain.
     *
     * @param keySharingId The ID of the key sharing to revoke
     */
    void revokeKeySharing(KeySharingId keySharingId);
    
    /**
     * Gets a data key for a user, creating one if it doesn't exist.
     *
     * @param userId The user to get a key for
     * @param tenantId The tenant the user belongs to
     * @return The data key ID
     */
    EncryptionKeyId getOrCreateDataKeyForUser(UserId userId, TenantId tenantId);
}
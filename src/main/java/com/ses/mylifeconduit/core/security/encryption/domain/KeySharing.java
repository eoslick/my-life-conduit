package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.ddd.domain.Entity;

import java.time.Instant;

/**
 * Represents a grant of access to a data key by one subject to another.
 * This is used to implement the key sharing mechanism with configurable expiration.
 */
public class KeySharing extends Entity<KeySharingId> {
    private final EncryptionKeyId dataKeyId;
    private final AccessSubject grantor;
    private final AccessSubject grantee;
    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;
    
    /**
     * Creates a new key sharing grant.
     *
     * @param id The unique identifier for this sharing grant
     * @param dataKeyId The ID of the data key being shared
     * @param grantor The subject granting access
     * @param grantee The subject receiving access
     * @param createdAt When the grant was created
     * @param expiresAt When the grant expires (null for permanent)
     */
    public KeySharing(
            KeySharingId id,
            EncryptionKeyId dataKeyId,
            AccessSubject grantor,
            AccessSubject grantee,
            Instant createdAt,
            Instant expiresAt) {
        super(id);
        this.dataKeyId = dataKeyId;
        this.grantor = grantor;
        this.grantee = grantee;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.revokedAt = null;
    }
    
    /**
     * Gets the ID of the data key being shared.
     *
     * @return The data key ID
     */
    public EncryptionKeyId getDataKeyId() {
        return dataKeyId;
    }
    
    /**
     * Gets the subject granting access.
     *
     * @return The grantor
     */
    public AccessSubject getGrantor() {
        return grantor;
    }
    
    /**
     * Gets the subject receiving access.
     *
     * @return The grantee
     */
    public AccessSubject getGrantee() {
        return grantee;
    }
    
    /**
     * Gets when the sharing grant was created.
     *
     * @return The creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Gets when the sharing grant expires.
     *
     * @return The expiration timestamp, or null if permanent
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    /**
     * Checks if the sharing grant has been revoked.
     *
     * @return True if revoked, false otherwise
     */
    public boolean isRevoked() {
        return revoked;
    }
    
    /**
     * Gets when the sharing grant was revoked.
     *
     * @return The revocation timestamp, or null if not revoked
     */
    public Instant getRevokedAt() {
        return revokedAt;
    }
    
    /**
     * Revokes this sharing grant.
     *
     * @param revokedAt When the grant was revoked
     */
    public void revoke(Instant revokedAt) {
        if (!revoked) {
            this.revoked = true;
            this.revokedAt = revokedAt;
        }
    }
    
    /**
     * Checks if this sharing grant is currently active (not expired or revoked).
     *
     * @param now The current timestamp to check against
     * @return True if the grant is active, false otherwise
     */
    public boolean isActive(Instant now) {
        return !revoked && (expiresAt == null || now.isBefore(expiresAt));
    }
}
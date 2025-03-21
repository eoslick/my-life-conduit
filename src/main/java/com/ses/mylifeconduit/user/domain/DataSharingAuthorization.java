package com.ses.mylifeconduit.user.domain;

import com.ses.mylifeconduit.core.ddd.domain.Entity;
import com.ses.mylifeconduit.core.security.encryption.domain.KeySharingId;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a subscriber's authorization to share encrypted data with another subject.
 * 
 * This entity belongs to the User domain and represents the business concept of 
 * authorizing data sharing, while the actual key sharing implementation is handled
 * in the Security/Encryption domain.
 */
public class DataSharingAuthorization extends Entity<KeySharingId> {
    private final SubscriberId ownerSubscriberId;
    private final UserId ownerUserId;
    private final SharingTarget target;
    private final String dataName;
    private final DataSharingPurpose purpose;
    private final Instant authorizedAt;
    private final Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;
    private String revocationReason;
    
    /**
     * Creates a new data sharing authorization.
     *
     * @param id The technical key sharing ID (from encryption domain)
     * @param ownerSubscriberId The subscriber authorizing the sharing
     * @param ownerUserId The system user ID of the owner
     * @param target The target recipient of the sharing
     * @param dataName A user-friendly name for the shared data
     * @param purpose The business purpose for sharing
     * @param authorizedAt When the sharing was authorized
     * @param expiresAt When the authorization expires
     */
    public DataSharingAuthorization(
            KeySharingId id,
            SubscriberId ownerSubscriberId,
            UserId ownerUserId,
            SharingTarget target,
            String dataName,
            DataSharingPurpose purpose,
            Instant authorizedAt,
            Instant expiresAt) {
        super(id);
        this.ownerSubscriberId = ownerSubscriberId;
        this.ownerUserId = ownerUserId;
        this.target = target;
        this.dataName = dataName;
        this.purpose = purpose;
        this.authorizedAt = authorizedAt;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.revokedAt = null;
        this.revocationReason = null;
    }
    
    /**
     * Gets the subscriber who authorized the sharing.
     *
     * @return The owner subscriber ID
     */
    public SubscriberId getOwnerSubscriberId() {
        return ownerSubscriberId;
    }
    
    /**
     * Gets the system user ID of the owner.
     *
     * @return The owner user ID
     */
    public UserId getOwnerUserId() {
        return ownerUserId;
    }
    
    /**
     * Gets the recipient of the sharing.
     *
     * @return The sharing target
     */
    public SharingTarget getTarget() {
        return target;
    }
    
    /**
     * Gets the user-friendly name for the shared data.
     *
     * @return The data name
     */
    public String getDataName() {
        return dataName;
    }
    
    /**
     * Gets the business purpose for the sharing.
     *
     * @return The sharing purpose
     */
    public DataSharingPurpose getPurpose() {
        return purpose;
    }
    
    /**
     * Gets when the sharing was authorized.
     *
     * @return The authorization timestamp
     */
    public Instant getAuthorizedAt() {
        return authorizedAt;
    }
    
    /**
     * Gets when the authorization expires.
     *
     * @return The expiration timestamp, or null if permanent
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    /**
     * Checks if the authorization has been revoked.
     *
     * @return True if revoked, false otherwise
     */
    public boolean isRevoked() {
        return revoked;
    }
    
    /**
     * Gets when the authorization was revoked.
     *
     * @return The revocation timestamp, or null if not revoked
     */
    public Instant getRevokedAt() {
        return revokedAt;
    }
    
    /**
     * Gets the reason for revocation.
     *
     * @return The revocation reason, or null if not provided or not revoked
     */
    public String getRevocationReason() {
        return revocationReason;
    }
    
    /**
     * Revokes this authorization.
     *
     * @param revokedAt When the authorization was revoked
     * @param reason The reason for revocation (optional)
     */
    public void revoke(Instant revokedAt, String reason) {
        if (!revoked) {
            this.revoked = true;
            this.revokedAt = revokedAt;
            this.revocationReason = reason;
        }
    }
    
    /**
     * Checks if this authorization is currently active (not expired or revoked).
     *
     * @param now The current timestamp to check against
     * @return True if the authorization is active, false otherwise
     */
    public boolean isActive(Instant now) {
        return !revoked && (expiresAt == null || now.isBefore(expiresAt));
    }
    
    /**
     * Creates a standard sharing with the default expiration of one week.
     *
     * @param id The technical key sharing ID
     * @param ownerSubscriberId The subscriber authorizing the sharing
     * @param ownerUserId The system user ID of the owner
     * @param target The target recipient of the sharing
     * @param dataName A user-friendly name for the shared data
     * @param purpose The business purpose for sharing
     * @param now The current timestamp
     * @return A new data sharing authorization
     */
    public static DataSharingAuthorization createStandardSharing(
            KeySharingId id,
            SubscriberId ownerSubscriberId,
            UserId ownerUserId,
            SharingTarget target,
            String dataName,
            DataSharingPurpose purpose,
            Instant now) {
        // Default expiration is one week
        Instant expiresAt = now.plus(Duration.ofDays(7));
        return new DataSharingAuthorization(id, ownerSubscriberId, ownerUserId, target, dataName, purpose, now, expiresAt);
    }
}
package com.ses.mylifeconduit.user.domain.events;

import com.ses.mylifeconduit.core.events.domain.AbstractDomainEvent;
import com.ses.mylifeconduit.core.events.domain.EventId;
import com.ses.mylifeconduit.core.security.encryption.domain.EncryptionKeyId;
import com.ses.mylifeconduit.core.security.encryption.domain.KeySharingId;
import com.ses.mylifeconduit.user.domain.DataSharingPurpose;
import com.ses.mylifeconduit.user.domain.SharingTarget;
import com.ses.mylifeconduit.user.domain.SubscriberId;
import com.ses.mylifeconduit.user.domain.UserId;

import java.time.Instant;

/**
 * Domain event representing a subscriber's authorization to share data.
 */
public class DataSharingAuthorized extends AbstractDomainEvent {
    private final KeySharingId sharingId;
    private final UserId ownerUserId;
    private final EncryptionKeyId dataKeyId;
    private final SharingTarget target;
    private final String dataName;
    private final DataSharingPurpose purpose;
    private final Instant expiresAt;
    
    /**
     * Creates a new data sharing authorized event.
     *
     * @param eventId The unique identifier for this event
     * @param occurredAt When the event occurred
     * @param subscriberId The subscriber who authorized the sharing
     * @param sharingId The ID of the sharing grant
     * @param ownerUserId The system user ID of the owner
     * @param dataKeyId The ID of the data key being shared
     * @param target The target recipient of the sharing
     * @param dataName A user-friendly name for the shared data
     * @param purpose The business purpose for sharing
     * @param expiresAt When the sharing expires
     */
    public DataSharingAuthorized(
            EventId eventId,
            Instant occurredAt,
            SubscriberId subscriberId,
            KeySharingId sharingId,
            UserId ownerUserId,
            EncryptionKeyId dataKeyId,
            SharingTarget target,
            String dataName,
            DataSharingPurpose purpose,
            Instant expiresAt) {
        super(eventId, occurredAt, subscriberId, 1);
        this.sharingId = sharingId;
        this.ownerUserId = ownerUserId;
        this.dataKeyId = dataKeyId;
        this.target = target;
        this.dataName = dataName;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }
    
    /**
     * Creates a new data sharing authorized event with current timestamp.
     *
     * @param subscriberId The subscriber who authorized the sharing
     * @param sharingId The ID of the sharing grant
     * @param ownerUserId The system user ID of the owner
     * @param dataKeyId The ID of the data key being shared
     * @param target The target recipient of the sharing
     * @param dataName A user-friendly name for the shared data
     * @param purpose The business purpose for sharing
     * @param expiresAt When the sharing expires
     */
    public DataSharingAuthorized(
            SubscriberId subscriberId,
            KeySharingId sharingId,
            UserId ownerUserId,
            EncryptionKeyId dataKeyId,
            SharingTarget target,
            String dataName,
            DataSharingPurpose purpose,
            Instant expiresAt) {
        super(subscriberId);
        this.sharingId = sharingId;
        this.ownerUserId = ownerUserId;
        this.dataKeyId = dataKeyId;
        this.target = target;
        this.dataName = dataName;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }
    
    public KeySharingId getSharingId() {
        return sharingId;
    }
    
    public UserId getOwnerUserId() {
        return ownerUserId;
    }
    
    public EncryptionKeyId getDataKeyId() {
        return dataKeyId;
    }
    
    public SharingTarget getTarget() {
        return target;
    }
    
    public String getDataName() {
        return dataName;
    }
    
    public DataSharingPurpose getPurpose() {
        return purpose;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
}
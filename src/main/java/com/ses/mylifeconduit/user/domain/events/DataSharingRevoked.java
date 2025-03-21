package com.ses.mylifeconduit.user.domain.events;

import com.ses.mylifeconduit.core.events.domain.AbstractDomainEvent;
import com.ses.mylifeconduit.core.events.domain.EventId;
import com.ses.mylifeconduit.core.security.encryption.domain.KeySharingId;
import com.ses.mylifeconduit.user.domain.SubscriberId;
import com.ses.mylifeconduit.user.domain.UserId;

import java.time.Instant;

/**
 * Domain event representing a subscriber's revocation of previously authorized data sharing.
 */
public class DataSharingRevoked extends AbstractDomainEvent {
    private final KeySharingId sharingId;
    private final UserId revokingUserId;
    private final String revocationReason;
    
    /**
     * Creates a new data sharing revoked event.
     *
     * @param eventId The unique identifier for this event
     * @param occurredAt When the event occurred
     * @param subscriberId The subscriber who revoked the sharing
     * @param sharingId The ID of the sharing grant being revoked
     * @param revokingUserId The system user ID of the person revoking
     * @param revocationReason The reason for revocation (optional)
     */
    public DataSharingRevoked(
            EventId eventId,
            Instant occurredAt,
            SubscriberId subscriberId,
            KeySharingId sharingId,
            UserId revokingUserId,
            String revocationReason) {
        super(eventId, occurredAt, subscriberId, 1);
        this.sharingId = sharingId;
        this.revokingUserId = revokingUserId;
        this.revocationReason = revocationReason;
    }
    
    /**
     * Creates a new data sharing revoked event with current timestamp.
     *
     * @param subscriberId The subscriber who revoked the sharing
     * @param sharingId The ID of the sharing grant being revoked
     * @param revokingUserId The system user ID of the person revoking
     * @param revocationReason The reason for revocation (optional)
     */
    public DataSharingRevoked(
            SubscriberId subscriberId,
            KeySharingId sharingId,
            UserId revokingUserId,
            String revocationReason) {
        super(subscriberId);
        this.sharingId = sharingId;
        this.revokingUserId = revokingUserId;
        this.revocationReason = revocationReason;
    }
    
    public KeySharingId getSharingId() {
        return sharingId;
    }
    
    public UserId getRevokingUserId() {
        return revokingUserId;
    }
    
    public String getRevocationReason() {
        return revocationReason;
    }
}
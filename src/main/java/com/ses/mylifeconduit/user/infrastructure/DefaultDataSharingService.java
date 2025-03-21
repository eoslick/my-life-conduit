package com.ses.mylifeconduit.user.infrastructure;

import com.ses.mylifeconduit.core.security.encryption.domain.EncryptionKeyId;
import com.ses.mylifeconduit.core.security.encryption.domain.EncryptionService;
import com.ses.mylifeconduit.core.security.encryption.domain.KeySharing;
import com.ses.mylifeconduit.core.security.encryption.domain.KeySharingId;
import com.ses.mylifeconduit.user.domain.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the DataSharingService.
 * 
 * This class demonstrates how the User domain interacts with the Encryption
 * domain to implement data sharing while maintaining separate responsibilities.
 */
public class DefaultDataSharingService implements DataSharingService {
    
    private final EncryptionService encryptionService;
    
    // This would typically be a repository in a real implementation
    private final Map<KeySharingId, DataSharingAuthorization> authorizations = new ConcurrentHashMap<>();
    
    /**
     * Creates a new data sharing service.
     *
     * @param encryptionService The encryption service to delegate key operations to
     */
    public DefaultDataSharingService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }
    
    @Override
    public DataSharingAuthorization authorizeUserSharing(
            UserId ownerUserId,
            EncryptionKeyId dataKeyId,
            UserId targetUserId,
            String dataName,
            DataSharingPurpose purpose,
            Duration duration) {
        
        // 1. Create the technical key sharing in the encryption domain
        KeySharing keySharing = encryptionService.createUserKeySharing(
                dataKeyId, ownerUserId, targetUserId, duration);
        
        // 2. Create and store the business authorization in the user domain
        Instant now = Instant.now();
        Instant expiresAt = duration != null ? now.plus(duration) : null;
        
        SharingTarget target = new SharingTarget.UserTarget(targetUserId);
        
        DataSharingAuthorization authorization = new DataSharingAuthorization(
                keySharing.getId(),
                ownerUserId,
                target,
                dataName,
                purpose,
                now,
                expiresAt);
        
        authorizations.put(authorization.getId(), authorization);
        
        return authorization;
    }
    
    @Override
    public DataSharingAuthorization authorizeRoleSharing(
            UserId ownerUserId,
            EncryptionKeyId dataKeyId,
            String roleName,
            String dataName,
            DataSharingPurpose purpose,
            Duration duration) {
        
        // 1. Create the technical key sharing in the encryption domain
        KeySharing keySharing = encryptionService.createRoleKeySharing(
                dataKeyId, ownerUserId, roleName, duration);
        
        // 2. Create and store the business authorization in the user domain
        Instant now = Instant.now();
        Instant expiresAt = duration != null ? now.plus(duration) : null;
        
        SharingTarget target = new SharingTarget.RoleTarget(roleName);
        
        DataSharingAuthorization authorization = new DataSharingAuthorization(
                keySharing.getId(),
                ownerUserId,
                target,
                dataName,
                purpose,
                now,
                expiresAt);
        
        authorizations.put(authorization.getId(), authorization);
        
        return authorization;
    }
    
    @Override
    public void revokeAuthorization(KeySharingId authorizationId, UserId revokingUserId) {
        DataSharingAuthorization authorization = authorizations.get(authorizationId);
        
        if (authorization != null) {
            // Verify the revoking user is the owner
            if (!authorization.getOwnerUserId().equals(revokingUserId)) {
                throw new IllegalArgumentException("Only the owner can revoke an authorization");
            }
            
            // 1. Revoke the technical key sharing in the encryption domain
            encryptionService.revokeKeySharing(authorizationId);
            
            // 2. Update the business authorization in the user domain
            authorization.revoke(Instant.now());
        }
    }
    
    @Override
    public Iterable<DataSharingAuthorization> listActiveAuthorizationsGrantedBy(UserId userId) {
        Instant now = Instant.now();
        List<DataSharingAuthorization> result = new ArrayList<>();
        
        for (DataSharingAuthorization auth : authorizations.values()) {
            if (auth.getOwnerUserId().equals(userId) && auth.isActive(now)) {
                result.add(auth);
            }
        }
        
        return result;
    }
    
    @Override
    public Iterable<DataSharingAuthorization> listActiveAuthorizationsGrantedTo(UserId userId) {
        Instant now = Instant.now();
        List<DataSharingAuthorization> result = new ArrayList<>();
        
        for (DataSharingAuthorization auth : authorizations.values()) {
            if (auth.getTarget() instanceof SharingTarget.UserTarget userTarget &&
                    userTarget.userId().equals(userId) &&
                    auth.isActive(now)) {
                result.add(auth);
            }
        }
        
        return result;
    }
}
package com.ses.mylifeconduit.user.domain;

import com.ses.mylifeconduit.core.tenant.TenantId;

/**
 * Represents a recipient of shared data, which can be a user, role, or tenant.
 * This is a value object in the User domain that abstracts the different
 * types of entities that can receive shared data.
 */
public sealed interface SharingTarget {
    
    /**
     * Gets the type of this sharing target.
     * 
     * @return The sharing target type
     */
    SharingTargetType getType();
    
    /**
     * Enum defining the types of entities that can receive shared data.
     */
    enum SharingTargetType {
        USER,
        ROLE,
        TENANT
    }
    
    /**
     * A user as a sharing target.
     * 
     * @param userId The ID of the user
     */
    record UserTarget(UserId userId) implements SharingTarget {
        @Override
        public SharingTargetType getType() {
            return SharingTargetType.USER;
        }
    }
    
    /**
     * A role as a sharing target.
     * 
     * @param roleName The name of the role
     */
    record RoleTarget(String roleName) implements SharingTarget {
        @Override
        public SharingTargetType getType() {
            return SharingTargetType.ROLE;
        }
    }
    
    /**
     * A tenant as a sharing target.
     * 
     * @param tenantId The ID of the tenant
     */
    record TenantTarget(TenantId tenantId) implements SharingTarget {
        @Override
        public SharingTargetType getType() {
            return SharingTargetType.TENANT;
        }
    }
}
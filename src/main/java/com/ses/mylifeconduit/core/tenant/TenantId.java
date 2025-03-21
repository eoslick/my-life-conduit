package com.ses.mylifeconduit.core.tenant;

import com.ses.mylifeconduit.core.ddd.domain.EntityId;

import java.util.UUID;

/**
 * Strongly-typed identifier for tenants in a multi-tenant system.
 */
public class TenantId extends EntityId {
    
    /**
     * Creates a new tenant identifier with a random UUID.
     */
    public TenantId() {
        super(UUID.randomUUID());
    }
    
    /**
     * Creates a tenant identifier with a specific UUID.
     * 
     * @param id The UUID to use for this identifier
     */
    public TenantId(UUID id) {
        super(id);
    }
}
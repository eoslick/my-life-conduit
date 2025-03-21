package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.ddd.domain.Entity;
import com.ses.mylifeconduit.core.tenant.TenantId;

/**
 * Represents a tenant master key in the envelope encryption model.
 * Each tenant has a unique master key used to wrap (encrypt) user data keys.
 */
public class MasterKey extends Entity<EncryptionKeyId> {
    private final byte[] keyMaterial;
    private final TenantId tenantId;
    
    /**
     * Creates a new master key.
     *
     * @param id The unique identifier for this key
     * @param keyMaterial The raw key material (stored securely)
     * @param tenantId The ID of the tenant this master key belongs to
     */
    public MasterKey(EncryptionKeyId id, byte[] keyMaterial, TenantId tenantId) {
        super(id);
        this.keyMaterial = keyMaterial;
        this.tenantId = tenantId;
    }
    
    /**
     * Gets the raw key material.
     * This should only be accessible within a secure context.
     *
     * @return The key material
     */
    public byte[] getKeyMaterial() {
        return keyMaterial;
    }
    
    /**
     * Gets the tenant ID this master key belongs to.
     *
     * @return The tenant ID
     */
    public TenantId getTenantId() {
        return tenantId;
    }
}
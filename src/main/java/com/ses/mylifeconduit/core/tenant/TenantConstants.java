// src/main/java/com/ses/mylifeconduit/core/tenant/TenantConstants.java
package com.ses.mylifeconduit.core.tenant;

import java.util.UUID;

/**
 * Holds constants related to Tenant management.
 */
public final class TenantConstants {

    private TenantConstants() {
        // Prevent instantiation
    }

    /**
     * The default TenantId used for single-tenant deployments or when no specific
     * tenant context is available. Using the nil UUID (all zeros).
     */
    public static final TenantId DEFAULT_TENANT_ID = new TenantId(new UUID(0L, 0L)); // UUID 00000000-0000-0000-0000-000000000000

    /**
     * Checks if the given TenantId is the default tenant ID.
     *
     * @param tenantId The TenantId to check.
     * @return true if the tenantId is the default one, false otherwise.
     */
    public static boolean isDefaultTenant(TenantId tenantId) {
        return DEFAULT_TENANT_ID.equals(tenantId);
    }
}
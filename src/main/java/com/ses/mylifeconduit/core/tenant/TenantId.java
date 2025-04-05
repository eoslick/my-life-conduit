// src/main/java/com/ses/mylifeconduit/core/tenant/TenantId.java
package com.ses.mylifeconduit.core.tenant;

import com.ses.mylifeconduit.core.ddd.EntityId;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents the strongly-typed identifier for a Tenant.
 * Uses a {@link UUID} as the underlying value.
 */
public record TenantId(UUID value) implements EntityId {

    /**
     * Creates a new TenantId with the specified UUID value.
     *
     * @param value The UUID value. Cannot be null.
     * @throws NullPointerException if value is null.
     */
    public TenantId {
        Objects.requireNonNull(value, "TenantId value cannot be null");
    }

    /**
     * Creates a new TenantId with a randomly generated UUID.
     *
     * @return A new TenantId instance.
     */
    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }

    /**
     * Creates a TenantId from a string representation of a UUID.
     *
     * @param uuidString The string representation of the UUID.
     * @return A TenantId instance.
     * @throws IllegalArgumentException if the string is not a valid UUID representation.
     */
    public static TenantId fromString(String uuidString) {
        Objects.requireNonNull(uuidString, "UUID string cannot be null");
        return new TenantId(UUID.fromString(uuidString));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
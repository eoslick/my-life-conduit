package com.ses.mylifeconduit.core.ddd.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all entity identifiers.
 * Provides a strongly-typed ID with UUID-based identity.
 */
public abstract class EntityId {
    private final UUID value;

    protected EntityId(UUID value) {
        this.value = Objects.requireNonNull(value, "ID value cannot be null");
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityId entityId = (EntityId) o;
        return Objects.equals(value, entityId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), value);
    }
}
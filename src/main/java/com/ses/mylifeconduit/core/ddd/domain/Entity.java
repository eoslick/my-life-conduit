package com.ses.mylifeconduit.core.ddd.domain;

import java.util.Objects;

/**
 * Base class for domain entities within an aggregate.
 * Entities are objects that have a distinct identity that runs through time
 * and different states. They are distinguished by their identity, not their attributes.
 * 
 * @param <ID> The strongly-typed identifier for this entity, must extend EntityId
 */
public abstract class Entity<ID extends EntityId> {
    private final ID id;

    /**
     * Constructs an entity with the given identifier.
     * 
     * @param id The strongly-typed unique identifier for this entity
     */
    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "Entity ID cannot be null");
    }

    /**
     * Returns the entity's unique identifier.
     * 
     * @return The strongly-typed identifier
     */
    public ID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Entity<?> entity = (Entity<?>) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
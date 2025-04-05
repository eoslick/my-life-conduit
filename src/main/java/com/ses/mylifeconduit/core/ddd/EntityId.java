// src/main/java/com/ses/mylifeconduit/core/ddd/EntityId.java
package com.ses.mylifeconduit.core.ddd;

import java.io.Serializable;

/**
 * A marker interface for strongly-typed identifiers used for Entities and Aggregate Roots.
 * <p>
 * Implementing classes (typically Java Records) should encapsulate the underlying identifier
 * (e.g., UUID, Long, String) and provide type safety throughout the domain.
 * <p>
 * Extending {@link Serializable} allows these IDs to be easily used in events, commands,
 * and potentially serialized state.
 */
public interface EntityId extends Serializable {

    /**
     * Gets the underlying raw value of the ID.
     * Use with caution, primarily for infrastructure concerns like persistence mapping.
     * Prefer using the strongly-typed ID object itself in domain and application logic.
     *
     * @return The raw ID value (e.g., UUID, String, Long).
     */
    Object value();
}
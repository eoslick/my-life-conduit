// src/main/java/com/ses/mylifeconduit/core/user/UserId.java
package com.ses.mylifeconduit.core.user;

import com.ses.mylifeconduit.core.ddd.EntityId;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents the strongly-typed identifier for a User.
 * Uses a {@link UUID} as the underlying value.
 */
public record UserId(UUID value) implements EntityId {

    /**
     * Creates a new UserId with the specified UUID value.
     *
     * @param value The UUID value. Cannot be null.
     * @throws NullPointerException if value is null.
     */
    public UserId {
        Objects.requireNonNull(value, "UserId value cannot be null");
    }

    /**
     * Creates a new UserId with a randomly generated UUID.
     *
     * @return A new UserId instance.
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * Creates a UserId from a string representation of a UUID.
     *
     * @param uuidString The string representation of the UUID.
     * @return A UserId instance.
     * @throws IllegalArgumentException if the string is not a valid UUID representation.
     */
    public static UserId fromString(String uuidString) {
        Objects.requireNonNull(uuidString, "UUID string cannot be null");
        return new UserId(UUID.fromString(uuidString));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
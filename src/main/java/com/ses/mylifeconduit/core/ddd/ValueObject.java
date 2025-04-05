// src/main/java/com/ses/mylifeconduit/core/ddd/ValueObject.java
package com.ses.mylifeconduit.core.ddd;

import java.io.Serializable;

/**
 * A marker interface for Value Objects in the Domain-Driven Design context.
 * <p>
 * Value Objects are objects defined by their attributes rather than a unique identity.
 * They should be treated as immutable. Two Value Objects are considered equal if all their
 * attributes are equal.
 * <p>
 * Implementing {@link Serializable} is often useful as Value Objects are frequently part
 * of Aggregates and Domain Events. Java Records are excellent candidates for implementing
 * Value Objects due to their inherent immutability and automatic implementation of
 * equals(), hashCode(), and toString() based on their components.
 */
public interface ValueObject extends Serializable {
    // Marker interface, no methods required by default.
    // Implementations (especially Records) provide necessary equality methods.
}
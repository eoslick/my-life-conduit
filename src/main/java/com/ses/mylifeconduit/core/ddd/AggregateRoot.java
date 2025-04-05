// src/main/java/com/ses/mylifeconduit/core/ddd/AggregateRoot.java
package com.ses.mylifeconduit.core.ddd;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
// Removed SLF4J imports
import java.lang.System.Logger; // Using Java's built-in System.Logger
import java.lang.System.Logger.Level; // Importing Level for convenience

/**
 * Base class for Aggregate Roots implementing Event Sourcing.
 * <p>
 * An aggregate is a cluster of domain objects (entities and value objects)
 * that can be treated as a single unit. An Aggregate Root is the entry point
 * to the aggregate, ensuring the integrity and invariants of the aggregate
 * as a whole.
 * <p>
 * This base class manages:
 * - The aggregate's unique identifier (ID).
 * - The current version (based on the number of applied events).
 * - A list of uncommitted changes (domain events).
 * - Applying events to mutate state via convention-based `apply(ConcreteDomainEvent event)` methods.
 *
 * @param <ID> The type of the Aggregate's unique identifier, extending {@link EntityId}.
 */
public abstract class AggregateRoot<ID extends EntityId> {

    // Using Java's built-in System.Logger to minimize external dependencies
    private static final Logger logger = System.getLogger(AggregateRoot.class.getName());

    private final ID id;
    private long version = 0L; // Version starts at 0 (no events applied yet)

    // List to hold events raised by command handlers but not yet persisted.
    private final transient List<DomainEvent> uncommittedChanges = new ArrayList<>();

    /**
     * Protected constructor for subclasses.
     *
     * @param id The unique identifier for this aggregate instance. Must not be null.
     */
    protected AggregateRoot(ID id) {
        Objects.requireNonNull(id, "Aggregate ID cannot be null");
        this.id = id;
    }

    /**
     * Gets the unique identifier of this aggregate instance.
     *
     * @return The aggregate ID.
     */
    public ID getId() {
        return id;
    }

    /**
     * Gets the current version of the aggregate.
     * This version reflects the number of events that have been successfully applied.
     *
     * @return The current version number.
     */
    public long getVersion() {
        return version;
    }

    /**
     * Gets an unmodifiable list of domain events that have been raised but not yet
     * committed to the event store.
     *
     * @return A list of uncommitted domain events.
     */
    public List<DomainEvent> getUncommittedChanges() {
        return Collections.unmodifiableList(uncommittedChanges);
    }

    /**
     * Clears the list of uncommitted changes.
     * Should typically be called by the repository after successfully persisting the events.
     */
    public void markChangesAsCommitted() {
        this.uncommittedChanges.clear();
    }

    /**
     * Loads the aggregate state from a history of domain events.
     * This is used during the reconstitution process.
     *
     * @param history The sequence of domain events representing the aggregate's history.
     */
    public final void loadFromHistory(Iterable<DomainEvent> history) {
        Objects.requireNonNull(history, "Event history cannot be null");
        history.forEach(event -> {
            if (event.aggregateVersion() != this.version + 1) {
                // Using System.Logger for error logging
                String errorMessage = String.format(
                        "Event version mismatch during replay in Aggregate %s (%s). Expected: %d, Got: %d for event %s (%s)",
                        this.id.value(),
                        this.getClass().getSimpleName(),
                        this.version + 1,
                        event.aggregateVersion(),
                        event.eventId(),
                        event.getClass().getSimpleName()
                );
                logger.log(Level.ERROR, errorMessage);
                throw new IllegalStateException(errorMessage); // Still throw exception
            }
            applyChange(event, false); // Apply event but don't add to uncommitted changes
        });
    }

    /**
     * Applies a domain event to the aggregate. This method should be called by command handlers
     * within the aggregate to signify a state change.
     * <p>
     * It internally calls the appropriate `apply(ConcreteDomainEvent)` method based on the
     * event type and adds the event to the list of uncommitted changes.
     *
     * @param event The domain event representing the change. Must not be null.
     */
    protected void raiseEvent(DomainEvent event) {
        Objects.requireNonNull(event, "Event cannot be null when raising");
        applyChange(event, true); // Apply and add to uncommitted changes
    }

    /**
     * Internal helper method to apply an event and update the aggregate's state and version.
     * Uses reflection to find and invoke the appropriate `apply(ConcreteDomainEvent)` method.
     *
     * @param event    The event to apply.
     * @param isNew    If true, the event is considered a new change and added to uncommittedChanges.
     *                 If false, it's being applied during history replay.
     */
    private void applyChange(DomainEvent event, boolean isNew) {
        try {
            // Convention: Look for a method named "apply" that takes the specific event type
            Method method = findApplyMethod(event.getClass());
            if (method == null) {
                // Using System.Logger for warning
                logger.log(Level.WARNING,
                        "No apply method found for event type {0} in aggregate {1}",
                        event.getClass().getSimpleName(), this.getClass().getSimpleName());
                // Decide if this is an error or just ignorable (e.g., for older event versions)
                // For now, we log a warning and continue. Could throw an exception.
            } else {
                method.setAccessible(true); // Allow calling protected/private apply methods
                method.invoke(this, event);
            }
        } catch (InvocationTargetException e) {
            // Exception occurred within the apply method itself
            logger.log(Level.ERROR, "Exception occurred during event application: " + event.getClass().getSimpleName(), e.getCause());
            throw new RuntimeException("Exception occurred during event application: " + event.getClass().getSimpleName(), e.getCause());
        } catch (IllegalAccessException e) {
            // Should not happen if setAccessible(true) works
            logger.log(Level.ERROR, "Could not access apply method for event: " + event.getClass().getSimpleName(), e);
            throw new RuntimeException("Could not access apply method for event: " + event.getClass().getSimpleName(), e);
        }

        // Only increment version and add to changes if it's a new event
        if (isNew) {
            uncommittedChanges.add(event);
        }
        // Version always increments when an event is successfully processed (replay or new)
        this.version++;
    }

    /**
     * Finds the 'apply' method for the specific event type using reflection.
     * Looks for a method with signature `apply(SpecificEventType event)`.
     * Caching could be added here for performance.
     */
    private Method findApplyMethod(Class<?> eventType) {
        // Walk up the class hierarchy of the aggregate root
        Class<?> currentAggregateClass = this.getClass();
        while (currentAggregateClass != null && currentAggregateClass != Object.class) {
            try {
                // Look for apply(SpecificEventClass)
                return currentAggregateClass.getDeclaredMethod("apply", eventType);
            } catch (NoSuchMethodException e) {
                // Method not found in this class, try the superclass
                currentAggregateClass = currentAggregateClass.getSuperclass();
            }
        }
        // No matching method found in the hierarchy
        return null;
    }

    // Abstract methods or common logic for all aggregates can be added here.
    // Subclasses MUST implement private/protected apply(SpecificDomainEvent event) methods
    // for each event type they handle to mutate their state. Example:
    //
    // protected void apply(GoalDefined event) {
    //     this.title = event.title(); // Assuming title is a field in the aggregate
    //     this.status = GoalStatus.DEFINED;
    // }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) o;
        return id.equals(that.id); // Aggregates are identified by their ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
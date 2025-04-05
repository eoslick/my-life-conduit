package com.ses.mylifeconduit.goalsetting.infrastructure.persistence;

// Import domain types from goalsetting context
import com.ses.mylifeconduit.goalsetting.domain.UserGoals;
import com.ses.mylifeconduit.goalsetting.domain.UserGoalsRepository;

// Import core types
import com.ses.mylifeconduit.core.ddd.DomainEvent;
import com.ses.mylifeconduit.core.encryption.EncryptedValue;
import com.ses.mylifeconduit.core.encryption.EncryptionService;
import com.ses.mylifeconduit.core.eventsourcing.EventStore;
import com.ses.mylifeconduit.core.eventsourcing.StoredEvent;
import com.ses.mylifeconduit.core.eventsourcing.exception.EventDeserializationException;
import com.ses.mylifeconduit.core.eventsourcing.exception.EventStoreException;
import com.ses.mylifeconduit.core.security.exception.DecryptionException;
import com.ses.mylifeconduit.core.security.exception.SecurityCoreException;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Event-sourced implementation of the UserGoalsRepository.
 * Uses an EventStore to persist and load UserGoals aggregates.
 */
public class EventSourcedUserGoalsRepository implements UserGoalsRepository {

    private static final Logger logger = System.getLogger(EventSourcedUserGoalsRepository.class.getName());

    private final EventStore eventStore;
    private final EncryptionService encryptionService;
    private final String aggregateType = UserGoals.class.getSimpleName(); // Define aggregate type string

    /**
     * Constructor for dependency injection.
     *
     * @param eventStore        The event store implementation.
     * @param encryptionService The service for decrypting event payloads.
     */
    public EventSourcedUserGoalsRepository(EventStore eventStore, EncryptionService encryptionService) {
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore cannot be null");
        this.encryptionService = Objects.requireNonNull(encryptionService, "encryptionService cannot be null");
        logger.log(Level.INFO, "EventSourcedUserGoalsRepository initialized.");
    }

    @Override
    public void save(UserGoals aggregate, TenantId tenantId, UserId actingUserId) {
        Objects.requireNonNull(aggregate, "aggregate cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");

        List<DomainEvent> events = aggregate.getUncommittedChanges();

        if (events.isEmpty()) {
            logger.log(Level.DEBUG, "No changes to save for UserGoals aggregate {0}", aggregate.getId());
            return;
        }

        UserId aggregateId = aggregate.getId();
        long expectedVersion = aggregate.getVersion() - events.size();

        logger.log(Level.DEBUG, "Saving {0} event(s) for aggregate {1} (type: {2}), expected version {3}, actor {4}",
                events.size(), aggregateId, this.aggregateType, expectedVersion, actingUserId);

        try {
            // Delegate persistence to the event store
            eventStore.appendEvents(aggregateId, this.aggregateType, expectedVersion, events, tenantId, actingUserId);

            // If successful, mark changes as committed
            aggregate.markChangesAsCommitted();
            logger.log(Level.DEBUG, "Successfully saved events for UserGoals aggregate {0}", aggregateId);

        } catch (EventStoreException | IllegalArgumentException e) {
            logger.log(Level.ERROR, "Failed to save events for UserGoals aggregate " + aggregateId, e);
            // Re-throw or wrap as needed
            throw e;
        }
    }

    @Override
    public Optional<UserGoals> findById(UserId userId, TenantId tenantId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        logger.log(Level.DEBUG, "Attempting to find UserGoals aggregate by ID {0}, tenant {1}", userId, tenantId);

        List<StoredEvent> storedEvents;
        try {
            storedEvents = eventStore.loadEventStream(userId, tenantId);
        } catch (EventStoreException e) {
            logger.log(Level.ERROR, "Failed to load event stream for UserGoals aggregate " + userId, e);
            throw e;
        }

        if (storedEvents.isEmpty()) {
            logger.log(Level.DEBUG, "No event stream found for UserGoals aggregate {0}", userId);
            return Optional.empty();
        }

        try {
            // Use the factory method for reconstitution
            UserGoals aggregate = UserGoals.forReconstitution(userId);

            List<DomainEvent> domainEvents = new ArrayList<>(storedEvents.size());
            for (StoredEvent storedEvent : storedEvents) {
                // Ensure aggregate type matches - important if multiple aggregate types share ID space (unlikely with UserId)
                if (!this.aggregateType.equals(storedEvent.aggregateType())) {
                    logger.log(Level.ERROR, "Type mismatch in event stream for {0}. Expected {1} but found {2} at sequence {3}",
                            userId, this.aggregateType, storedEvent.aggregateType(), storedEvent.sequenceNumber());
                    throw new EventStoreException("Invalid event type found in stream for aggregate " + userId);
                }
                DomainEvent domainEvent = decryptAndDeserialize(storedEvent);
                domainEvents.add(domainEvent);
            }

            // Replay the history onto the new aggregate instance
            aggregate.loadFromHistory(domainEvents);

            logger.log(Level.DEBUG, "Successfully reconstituted UserGoals aggregate {0} to version {1}",
                    aggregate.getId(), aggregate.getVersion());
            return Optional.of(aggregate);

        } catch (DecryptionException | EventDeserializationException e) {
            logger.log(Level.ERROR, "Failed to decrypt or deserialize event during reconstitution for UserGoals aggregate " + userId, e);
            throw new EventStoreException("Failed to reconstitute aggregate " + userId + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed during event replay for UserGoals aggregate " + userId, e);
            throw new EventStoreException("Failed to apply event history for aggregate " + userId + ": " + e.getMessage(), e);
        }
    }

    // --- Helper Methods ---

    /**
     * Decrypts and deserializes the payload of a StoredEvent.
     * (Identical to the helper in EventSourcedUserCoreValuesRepository)
     */
    private DomainEvent decryptAndDeserialize(StoredEvent storedEvent)
            throws DecryptionException, EventDeserializationException {

        logger.log(Level.TRACE, "Decrypting payload for stored event sequence {0}, type {1}",
                storedEvent.sequenceNumber(), storedEvent.eventType());

        EncryptedValue<DomainEvent> encryptedValue = new EncryptedValue<>(
                storedEvent.getEventPayload(),
                storedEvent.encryptionAlgorithmId(),
                storedEvent.keyContextId()
        );

        try {
            DomainEvent decryptedEvent = encryptionService.decrypt(encryptedValue);
            if (decryptedEvent == null) {
                throw new EventDeserializationException("Decryption/Deserialization returned null for event sequence " + storedEvent.sequenceNumber());
            }
            return decryptedEvent;
        } catch (SecurityCoreException e) {
            logger.log(Level.ERROR, "Decryption/Deserialization failed for event sequence {0} (Type: {1}, KeyContext: {2})",
                    storedEvent.sequenceNumber(), storedEvent.eventType(), storedEvent.keyContextId(), e);
            if (e instanceof DecryptionException) throw (DecryptionException) e;
            if (e.getCause() instanceof ClassCastException) throw new EventDeserializationException("Deserialized event is not of expected type for sequence " + storedEvent.sequenceNumber(), e);
            throw new DecryptionException("Decryption failed for event sequence " + storedEvent.sequenceNumber() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Unexpected error during decryption/deserialization for event sequence {0}", storedEvent.sequenceNumber(), e);
            throw new EventDeserializationException("Unexpected error during event processing: " + e.getMessage(), e);
        }
    }
}
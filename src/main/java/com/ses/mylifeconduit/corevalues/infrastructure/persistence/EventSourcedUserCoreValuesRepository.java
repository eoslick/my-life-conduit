// --- File: com/ses/mylifeconduit/corevalues/infrastructure/persistence/EventSourcedUserCoreValuesRepository.java ---

package com.ses.mylifeconduit.corevalues.infrastructure.persistence;

import com.ses.mylifeconduit.core.ddd.DomainEvent;
import com.ses.mylifeconduit.core.encryption.EncryptedValue;
import com.ses.mylifeconduit.core.encryption.EncryptionService;
import com.ses.mylifeconduit.core.eventsourcing.EventStore;
import com.ses.mylifeconduit.core.eventsourcing.StoredEvent;
import com.ses.mylifeconduit.core.eventsourcing.exception.EventDeserializationException;
import com.ses.mylifeconduit.core.eventsourcing.exception.EventStoreException;
import com.ses.mylifeconduit.core.security.exception.DecryptionException; // For handling decryption errors
import com.ses.mylifeconduit.core.security.exception.SecurityCoreException; // Catch base exception
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;
import com.ses.mylifeconduit.corevalues.domain.UserCoreValues;
import com.ses.mylifeconduit.corevalues.domain.UserCoreValuesRepository;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Event-sourced implementation of the UserCoreValuesRepository.
 * Uses an EventStore to persist and load aggregates.
 */
public class EventSourcedUserCoreValuesRepository implements UserCoreValuesRepository {

    private static final Logger logger = System.getLogger(EventSourcedUserCoreValuesRepository.class.getName());

    private final EventStore eventStore;
    private final EncryptionService encryptionService;

    /**
     * Constructor for dependency injection.
     */
    public EventSourcedUserCoreValuesRepository(EventStore eventStore, EncryptionService encryptionService) {
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore cannot be null");
        this.encryptionService = Objects.requireNonNull(encryptionService, "encryptionService cannot be null");
        logger.log(Level.INFO, "EventSourcedUserCoreValuesRepository initialized.");
    }

    @Override
    public void save(UserCoreValues aggregate, TenantId tenantId, UserId actingUserId) {
        Objects.requireNonNull(aggregate, "aggregate cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");

        List<DomainEvent> events = aggregate.getUncommittedChanges();

        if (events.isEmpty()) {
            logger.log(Level.DEBUG, "No changes to save for aggregate {0}", aggregate.getId());
            return;
        }

        UserId aggregateId = aggregate.getId();
        String aggregateType = UserCoreValues.class.getSimpleName();
        long expectedVersion = aggregate.getVersion() - events.size();

        logger.log(Level.DEBUG, "Saving {0} event(s) for aggregate {1} (type: {2}), expected version {3}, actor {4}",
                events.size(), aggregateId, aggregateType, expectedVersion, actingUserId);

        try {
            eventStore.appendEvents(aggregateId, aggregateType, expectedVersion, events, tenantId, actingUserId);
            aggregate.markChangesAsCommitted();
            logger.log(Level.DEBUG, "Successfully saved events for aggregate {0}", aggregateId);

        } catch (EventStoreException | IllegalArgumentException e) {
            logger.log(Level.ERROR, "Failed to save events for aggregate " + aggregateId, e);
            throw e;
        }
    }

    @Override
    public Optional<UserCoreValues> findById(UserId userId, TenantId tenantId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        logger.log(Level.DEBUG, "Attempting to find aggregate by ID {0}, tenant {1}", userId, tenantId);

        List<StoredEvent> storedEvents;
        try {
            storedEvents = eventStore.loadEventStream(userId, tenantId);
        } catch (EventStoreException e) {
            logger.log(Level.ERROR, "Failed to load event stream for aggregate " + userId, e);
            throw e;
        }

        if (storedEvents.isEmpty()) {
            logger.log(Level.DEBUG, "No event stream found for aggregate {0}", userId);
            return Optional.empty();
        }

        try {
            UserCoreValues aggregate = UserCoreValues.forReconstitution(userId);

            List<DomainEvent> domainEvents = new ArrayList<>(storedEvents.size());
            for (StoredEvent storedEvent : storedEvents) {
                DomainEvent domainEvent = decryptAndDeserialize(storedEvent);
                domainEvents.add(domainEvent);
            }

            aggregate.loadFromHistory(domainEvents);

            logger.log(Level.DEBUG, "Successfully reconstituted aggregate {0} to version {1}",
                    aggregate.getId(), aggregate.getVersion());
            return Optional.of(aggregate);

        } catch (DecryptionException | EventDeserializationException e) {
            logger.log(Level.ERROR, "Failed to decrypt or deserialize event during reconstitution for aggregate " + userId, e);
            throw new EventStoreException("Failed to reconstitute aggregate " + userId + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed during event replay for aggregate " + userId, e);
            throw new EventStoreException("Failed to apply event history for aggregate " + userId + ": " + e.getMessage(), e);
        }
    }

    // --- Helper Methods ---

    /**
     * Decrypts and deserializes the payload of a StoredEvent.
     */
    private DomainEvent decryptAndDeserialize(StoredEvent storedEvent)
            throws DecryptionException, EventDeserializationException {

        logger.log(Level.TRACE, "Decrypting payload for stored event sequence {0}, type {1}",
                storedEvent.sequenceNumber(), storedEvent.eventType());

        // Create EncryptedValue specifying the expected DomainEvent type parameter
        EncryptedValue<DomainEvent> encryptedValue = new EncryptedValue<>(
                storedEvent.getEventPayload(),
                storedEvent.encryptionAlgorithmId(),
                storedEvent.keyContextId()
        );

        try {
            // EncryptionService.decrypt now receives EncryptedValue<DomainEvent>
            DomainEvent decryptedEvent = encryptionService.decrypt(encryptedValue);

            if (decryptedEvent == null) {
                throw new EventDeserializationException("Decryption/Deserialization returned null for event sequence " + storedEvent.sequenceNumber());
            }
            return decryptedEvent;

        } catch (SecurityCoreException e) {
            logger.log(Level.ERROR, "Decryption/Deserialization failed for event sequence {0} (Type: {1}, KeyContext: {2})",
                    storedEvent.sequenceNumber(), storedEvent.eventType(), storedEvent.keyContextId(), e);
            if (e instanceof DecryptionException) {
                throw (DecryptionException) e;
            }
            if (e.getCause() instanceof ClassCastException) {
                throw new EventDeserializationException("Deserialized event is not of expected type for sequence " + storedEvent.sequenceNumber(), e);
            }
            throw new DecryptionException("Decryption failed for event sequence " + storedEvent.sequenceNumber() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Unexpected error during decryption/deserialization for event sequence {0}", storedEvent.sequenceNumber(), e);
            throw new EventDeserializationException("Unexpected error during event processing: " + e.getMessage(), e);
        }
    }
}
// --- End File ---
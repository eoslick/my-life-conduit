// --- File: com/ses/mylifeconduit/infrastructure/eventsourcing/JdbcEventStore.java ---

package com.ses.mylifeconduit.infrastructure.eventsourcing;

import com.ses.mylifeconduit.core.ddd.DomainEvent;
import com.ses.mylifeconduit.core.ddd.EntityId;
import com.ses.mylifeconduit.core.encryption.EncryptedValue;
import com.ses.mylifeconduit.core.encryption.EncryptionService;
import com.ses.mylifeconduit.core.encryption.KeyContext;
import com.ses.mylifeconduit.core.eventsourcing.EventStore;
import com.ses.mylifeconduit.core.eventsourcing.StoredEvent;
import com.ses.mylifeconduit.core.eventsourcing.exception.ConcurrencyException;
import com.ses.mylifeconduit.core.eventsourcing.exception.EventStoreException;
import com.ses.mylifeconduit.core.security.exception.SecurityCoreException; // For encryption errors
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;
import com.ses.mylifeconduit.infrastructure.persistence.h2.H2DatabaseInitializer; // For default URL

import java.io.*; // For Serialization IOException and @Serial
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.*; // JDBC classes
import java.time.Instant;
import java.util.*; // List, ArrayList, UUID, Objects

/**
 * JDBC implementation of the EventStore interface using an H2 database
 * and collaborating with an EncryptionService.
 */
public class JdbcEventStore implements EventStore {

    private static final Logger logger = System.getLogger(JdbcEventStore.class.getName());

    private final String dbUrl;
    private final EncryptionService encryptionService;

    // SQL Statements for EVENT_STORE table
    private static final String INSERT_EVENT_SQL = """
        INSERT INTO EVENT_STORE (
            event_id, aggregate_id, aggregate_type, tenant_id, sequence_number,
            event_type, event_payload, event_version, encryption_algorithm_id,
            key_context_id, occurred_on, user_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String SELECT_STREAM_SQL = """
        SELECT
            event_id, aggregate_id, aggregate_type, tenant_id, sequence_number,
            event_type, event_payload, event_version, encryption_algorithm_id,
            key_context_id, occurred_on, stored_on, user_id
        FROM EVENT_STORE
        WHERE aggregate_id = ? AND tenant_id = ?
        ORDER BY sequence_number ASC
        """;

    private static final String SELECT_STREAM_AFTER_SQL = """
        SELECT
            event_id, aggregate_id, aggregate_type, tenant_id, sequence_number,
            event_type, event_payload, event_version, encryption_algorithm_id,
            key_context_id, occurred_on, stored_on, user_id
        FROM EVENT_STORE
        WHERE aggregate_id = ? AND tenant_id = ? AND sequence_number > ?
        ORDER BY sequence_number ASC
        """;

    // Query for concurrency check
    private static final String SELECT_CURRENT_VERSION_SQL = """
        SELECT MAX(sequence_number) FROM EVENT_STORE
        WHERE aggregate_id = ? AND tenant_id = ?
        """;

    // H2 specific SQLState for unique constraint violation (UQ_AGGREGATE_VERSION)
    private static final String H2_UNIQUE_CONSTRAINT_VIOLATION_STATE = "23505";


    /**
     * Constructor with dependencies.
     */
    public JdbcEventStore(String dbUrl, EncryptionService encryptionService) {
        this.dbUrl = Objects.requireNonNull(dbUrl, "dbUrl cannot be null");
        this.encryptionService = Objects.requireNonNull(encryptionService, "encryptionService cannot be null");
        logger.log(Level.INFO, "JdbcEventStore initialized for DB URL (sanitized): {0} with EncryptionService: {1}",
                sanitizeJdbcUrl(this.dbUrl), encryptionService.getClass().getSimpleName());
    }

    /**
     * Convenience constructor using default H2 App DB URL.
     */
    public JdbcEventStore(EncryptionService encryptionService) {
        this(H2DatabaseInitializer.DEFAULT_APP_DB_URL, encryptionService);
    }

    // --- Helper: Get DB Connection ---
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.dbUrl);
    }

    // --- EventStore Implementation ---

    @Override
    public void appendEvents(
            EntityId aggregateId,
            String aggregateType,
            long expectedVersion,
            List<? extends DomainEvent> events,
            TenantId tenantId,
            UserId userId
    ) {
        Objects.requireNonNull(aggregateId, "aggregateId cannot be null");
        Objects.requireNonNull(aggregateType, "aggregateType cannot be null");
        Objects.requireNonNull(events, "events list cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        if (events.isEmpty()) {
            logger.log(Level.WARNING, "appendEvents called with empty event list for aggregate {0}", aggregateId.value().toString());
            return;
        }

        logger.log(Level.DEBUG, "Attempting to append {0} event(s) for aggregate {1} ({2}), expected version {3}",
                events.size(), aggregateId.value().toString(), aggregateType, expectedVersion);

        Connection conn = null;
        boolean committed = false;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            if (expectedVersion >= 0) {
                long currentVersion = getCurrentAggregateVersion(conn, aggregateId, tenantId);
                if (expectedVersion == 0 && currentVersion > 0) {
                    throw new ConcurrencyException( String.format("Concurrency conflict: Aggregate %s (%s) already exists. Expected version 0, found %d.", aggregateId.value().toString(), aggregateType, currentVersion), aggregateId, expectedVersion, currentVersion);
                } else if (expectedVersion > 0 && currentVersion != expectedVersion) {
                    throw new ConcurrencyException( String.format("Concurrency conflict for aggregate %s (%s). Expected version: %d, Actual version: %d", aggregateId.value().toString(), aggregateType, expectedVersion, currentVersion), aggregateId, expectedVersion, currentVersion);
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(INSERT_EVENT_SQL)) {
                long sequence = expectedVersion;

                for (DomainEvent event : events) {
                    sequence++;
                    if (event.aggregateVersion() != sequence) {
                        throw new EventStoreException(String.format( "Event sequence mismatch in batch for aggregate %s. Event %s (%s) has version %d, expected %d.", aggregateId.value().toString(), event.getClass().getSimpleName(), event.eventId(), event.aggregateVersion(), sequence ));
                    }

                    byte[] serializedPayload = serializeEvent(event);
                    KeyContext keyContext = KeyContext.newContext(tenantId, userId);
                    EncryptedValue<?> encryptedValue = encryptPayload(serializedPayload, keyContext);

                    pstmt.setObject(1, UUID.randomUUID());
                    pstmt.setString(2, aggregateId.value().toString());
                    pstmt.setString(3, aggregateType);
                    pstmt.setObject(4, tenantId.value());
                    pstmt.setLong(5, sequence);
                    pstmt.setString(6, event.getClass().getName());
                    pstmt.setBytes(7, encryptedValue.encryptedData());
                    pstmt.setString(8, event.eventVersion());
                    pstmt.setString(9, encryptedValue.algorithmId());
                    pstmt.setObject(10, encryptedValue.dekContextId());
                    pstmt.setTimestamp(11, Timestamp.from(event.occurredOn()));
                    pstmt.setObject(12, userId.value());

                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
            committed = true;
            logger.log(Level.DEBUG, "Successfully committed {0} events for aggregate {1}", events.size(), aggregateId.value().toString());

        } catch (SQLException e) {
            if (H2_UNIQUE_CONSTRAINT_VIOLATION_STATE.equals(e.getSQLState())) {
                logger.log(Level.WARNING, "Concurrency conflict detected (DB constraint) during event append for aggregate {0}", aggregateId.value().toString());
                throw new ConcurrencyException( "Concurrency conflict detected (DB constraint) during event append for aggregate " + aggregateId.value().toString(), e, aggregateId, expectedVersion);
            } else {
                logger.log(Level.ERROR, "SQL error appending events for aggregate " + aggregateId.value().toString(), e);
                throw new EventStoreException("Database error appending events", e);
            }
        } catch (IOException e) {
            logger.log(Level.ERROR, "Serialization error appending events for aggregate " + aggregateId.value().toString(), e);
            throw new EventStoreException("Failed to serialize event payload", e);
        } catch (EventStoreException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.ERROR, "Unexpected error appending events for aggregate " + aggregateId.value().toString(), e);
            throw new EventStoreException("Unexpected error appending events", e);
        } finally {
            if (conn != null) {
                try {
                    if (!committed && !conn.getAutoCommit()) {
                        logger.log(Level.DEBUG, "Rolling back transaction in finally block for appendEvents.");
                        conn.rollback();
                    }
                } catch (SQLException ex) {
                    logger.log(Level.ERROR, "Failed to rollback transaction in finally block", ex);
                } finally {
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        logger.log(Level.ERROR, "Failed to close connection", ex);
                    }
                }
            }
        }
    }


    @Override
    public List<StoredEvent> loadEventStream(EntityId aggregateId, TenantId tenantId) {
        return loadEventStreamAfter(aggregateId, tenantId, 0L);
    }

    @Override
    public List<StoredEvent> loadEventStreamAfter(EntityId aggregateId, TenantId tenantId, long afterVersion) {
        Objects.requireNonNull(aggregateId, "aggregateId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        if (afterVersion < 0) {
            throw new IllegalArgumentException("afterVersion cannot be negative");
        }

        logger.log(Level.DEBUG, "Loading event stream for aggregate {0}, tenant {1}, after version {2}",
                aggregateId.value().toString(), tenantId.value(), afterVersion);

        List<StoredEvent> eventStream = new ArrayList<>();
        String sql = (afterVersion == 0) ? SELECT_STREAM_SQL : SELECT_STREAM_AFTER_SQL;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, aggregateId.value().toString());
            pstmt.setObject(2, tenantId.value());
            if (afterVersion > 0) {
                pstmt.setLong(3, afterVersion);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    StoredEvent storedEvent = mapResultSetToStoredEvent(rs);
                    eventStream.add(storedEvent);
                }
            }
            logger.log(Level.DEBUG, "Loaded {0} event(s) for aggregate {1}", eventStream.size(), aggregateId.value().toString());

        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to load event stream for aggregate " + aggregateId.value().toString(), e);
            throw new EventStoreException("Database error loading event stream", e);
        }
        return eventStream;
    }

    // --- Helper Methods ---

    private EncryptedValue<?> encryptPayload(byte[] serializedPayload, KeyContext keyContext) throws SecurityCoreException {
        logger.log(Level.TRACE, "Encrypting payload using service: {0}", encryptionService.getClass().getSimpleName());
        try {
            return encryptionService.encrypt(new SerializableByteArray(serializedPayload), keyContext);
        } catch (SecurityCoreException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityCoreException("Failed during payload encryption step", e);
        }
    }

    private static record SerializableByteArray(byte[] data) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    private byte[] serializeEvent(DomainEvent event) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(event);
            return bos.toByteArray();
        }
    }

    private StoredEvent mapResultSetToStoredEvent(ResultSet rs) throws SQLException {
        UUID eventId = rs.getObject("event_id", UUID.class);
        EntityId aggregateId = new StringEntityId(rs.getString("aggregate_id")); // Placeholder
        String aggregateType = rs.getString("aggregate_type");
        TenantId tenantId = new TenantId(rs.getObject("tenant_id", UUID.class));
        long sequenceNumber = rs.getLong("sequence_number");
        String eventType = rs.getString("event_type");
        byte[] eventPayload = rs.getBytes("event_payload");
        String eventVersion = rs.getString("event_version");
        String encryptionAlgorithmId = rs.getString("encryption_algorithm_id");
        UUID keyContextId = rs.getObject("key_context_id", UUID.class);
        Instant occurredOn = rs.getTimestamp("occurred_on").toInstant();
        Instant storedOn = rs.getTimestamp("stored_on").toInstant();
        UserId userId = new UserId(rs.getObject("user_id", UUID.class));

        return new StoredEvent( eventId, aggregateId, aggregateType, tenantId, sequenceNumber, eventType, eventPayload, eventVersion, encryptionAlgorithmId, keyContextId, occurredOn, storedOn, userId );
    }

    /** Placeholder EntityId implementation for mapping. Requires proper implementation. */
    private record StringEntityId(String value) implements EntityId {
        // No explicit value() override needed; implicit String value() satisfies EntityId contract.
        @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; StringEntityId that = (StringEntityId) o; return Objects.equals(value, that.value); }
        @Override public int hashCode() { return Objects.hash(value); }
        @Override public String toString() { return value; }
    }


    private long getCurrentAggregateVersion(Connection conn, EntityId aggregateId, TenantId tenantId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_CURRENT_VERSION_SQL)) {
            pstmt.setString(1, aggregateId.value().toString());
            pstmt.setObject(2, tenantId.value());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    logger.log(Level.ERROR, "Unexpected result from MAX(sequence_number) query for aggregate {0}", aggregateId.value().toString());
                    return 0L;
                }
            }
        }
    }

    private static String sanitizeJdbcUrl(String url) {
        if (url == null) return null;
        return url.replaceAll("(?i)(user(?:name)?|password|secret)=[^;\\s&]+", "$1=***");
    }

}
// --- End File ---
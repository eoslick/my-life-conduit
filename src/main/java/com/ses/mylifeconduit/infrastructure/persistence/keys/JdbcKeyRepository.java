// --- File: src/main/java/com/ses/mylifeconduit/infrastructure/persistence/keys/JdbcKeyRepository.java ---
package com.ses.mylifeconduit.infrastructure.persistence.keys;

import com.ses.mylifeconduit.core.security.exception.KeyManagementException;
import com.ses.mylifeconduit.core.security.keys.*; // Import DekId, StoredWrappedDek, StoredUserKey etc.
import com.ses.mylifeconduit.core.security.sharing.GranteeType;
import com.ses.mylifeconduit.core.security.sharing.ShareGrantDetails;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;
import com.ses.mylifeconduit.infrastructure.persistence.h2.H2DatabaseInitializer; // For DB URL constant

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.*; // Import JDBC classes
import java.time.Instant;
import java.util.*; // Import List, Optional, Objects etc.
import java.util.regex.Matcher; // Import regex classes
import java.util.regex.Pattern;

/**
 * JDBC implementation of the KeyRepository interface for H2 database.
 * Manages User Keys, Wrapped DEKs, and Share Grants persistence.
 */
public class JdbcKeyRepository implements KeyRepository {

    private static final Logger logger = System.getLogger(JdbcKeyRepository.class.getName());
    private final String dbUrl;

    // Pattern to find common credential parameters in JDBC URLs
    private static final Pattern CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)(user(?:name)?|password|secret)=([^;\\s&]+)"
    );

    // --- SQL Statements ---

    // USER_KEYS Table
    private static final String MERGE_USER_KEY_SQL = """
        MERGE INTO USER_KEYS (user_id, tenant_id, wrapped_data_key, master_key_id, algorithm_id, updated_at)
        KEY(user_id, tenant_id) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
    private static final String FIND_USER_KEY_SQL = """
        SELECT wrapped_data_key, master_key_id, algorithm_id FROM USER_KEYS WHERE user_id = ? AND tenant_id = ?
        """;

    // WRAPPED_DEKS Table
    private static final String INSERT_WRAPPED_DEK_SQL = """
        INSERT INTO WRAPPED_DEKS (dek_id, owner_user_id, tenant_id, wrapped_dek, owner_key_id, algorithm_id, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String FIND_WRAPPED_DEK_BY_ID_SQL = """
        SELECT owner_user_id, tenant_id, wrapped_dek, owner_key_id, algorithm_id, created_at
        FROM WRAPPED_DEKS WHERE dek_id = ? AND tenant_id = ?
        """;

    // SHARE_GRANTS Table
    private static final String MERGE_SHARE_GRANT_SQL = """
        MERGE INTO SHARE_GRANTS (share_grant_id, tenant_id, owner_user_id, data_reference, grantee_type, grantee_id,
                                encrypted_data_key, grantee_key_id, algorithm_id, expiration_timestamp, created_timestamp)
        KEY(share_grant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
    private static final String FIND_GRANT_BY_ID_SQL = """
        SELECT tenant_id, owner_user_id, data_reference, grantee_type, grantee_id, encrypted_data_key,
               grantee_key_id, algorithm_id, expiration_timestamp, created_timestamp
        FROM SHARE_GRANTS WHERE share_grant_id = ? AND tenant_id = ?
        """;
    // Updated query to find grants for a specific DEK (using data_reference) and user
    private static final String FIND_ACTIVE_GRANTS_FOR_DEK_SQL = """
        SELECT share_grant_id, tenant_id, owner_user_id, data_reference, grantee_type, grantee_id,
               encrypted_data_key, grantee_key_id, algorithm_id, expiration_timestamp, created_timestamp
        FROM SHARE_GRANTS
        WHERE tenant_id = ?          -- Param 1: Tenant UUID
          AND data_reference = ?     -- Param 2: DekId UUID String
          AND grantee_type = ?       -- Param 3: Grantee Type (e.g., 'USER')
          AND grantee_id = ?         -- Param 4: Grantee ID (e.g., accessing User ID String)
          AND (expiration_timestamp IS NULL OR expiration_timestamp > CURRENT_TIMESTAMP) -- Check not expired
        ORDER BY created_timestamp DESC
        """;
    private static final String DELETE_GRANT_BY_ID_SQL = """
        DELETE FROM SHARE_GRANTS WHERE share_grant_id = ? AND tenant_id = ?
        """;
    private static final String FIND_EXPIRED_GRANTS_SQL = """
        SELECT share_grant_id, tenant_id, owner_user_id, data_reference, grantee_type, grantee_id,
               encrypted_data_key, grantee_key_id, algorithm_id, expiration_timestamp, created_timestamp
        FROM SHARE_GRANTS WHERE expiration_timestamp IS NOT NULL AND expiration_timestamp < ?
        ORDER BY expiration_timestamp ASC
        """;

    /**
     * Constructor. Uses the default H2 keys database URL.
     */
    public JdbcKeyRepository() {
        this(H2DatabaseInitializer.DEFAULT_KEYS_DB_URL);
    }

    /**
     * Constructor allowing injection of the database URL.
     * Logs a sanitized version of the URL.
     * @param dbUrl The JDBC URL for the keys database.
     */
    public JdbcKeyRepository(String dbUrl) {
        this.dbUrl = dbUrl;
        logger.log(Level.INFO, "JdbcKeyRepository initialized for URL: {0}", sanitizeJdbcUrl(this.dbUrl));
        // Consider centralizing DB init call, e.g., in MainApplication or DI container setup.
    }

    /**
     * Sanitizes a JDBC URL string for safe logging.
     */
    private static String sanitizeJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        Matcher matcher = CREDENTIAL_PATTERN.matcher(url);
        return matcher.replaceAll(matchResult -> matchResult.group(1) + "=***");
    }

    /**
     * Gets a database connection. Replace with DataSource for production.
     */
    private Connection getConnection() throws SQLException {
        // TODO: Replace DriverManager with a pooled DataSource in production applications.
        return DriverManager.getConnection(this.dbUrl);
    }

    // --- KeyRepository Implementation ---

    // --- User Key Methods ---
    @Override
    public void saveWrappedUserKey(TenantId tenantId, UserId userId, byte[] wrappedKey, String masterKeyId, String algorithmId) {
        logger.log(Level.DEBUG, "Saving wrapped user key for user {0} in tenant {1}", userId, tenantId);
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(MERGE_USER_KEY_SQL)) {

            pstmt.setObject(1, userId.value());      // user_id (UUID)
            pstmt.setObject(2, tenantId.value());    // tenant_id (UUID)
            pstmt.setBytes(3, wrappedKey);           // wrapped_data_key (VARBINARY)
            pstmt.setString(4, masterKeyId);         // master_key_id (VARCHAR)
            pstmt.setString(5, algorithmId);         // algorithm_id (VARCHAR)

            pstmt.executeUpdate();
            logger.log(Level.TRACE, "Saved/Updated user key for user {0}", userId);

        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to save wrapped user key for user " + userId, e);
            throw new KeyManagementException("Persistence error saving user key", e);
        }
    }

    @Override
    public Optional<StoredUserKey> findWrappedUserKey(TenantId tenantId, UserId userId) {
        logger.log(Level.DEBUG, "Finding wrapped user key for user {0} in tenant {1}", userId, tenantId);
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_USER_KEY_SQL)) {

            pstmt.setObject(1, userId.value());   // WHERE user_id = ?
            pstmt.setObject(2, tenantId.value()); // AND tenant_id = ?

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    StoredUserKey storedKey = new StoredUserKey(
                            rs.getBytes("wrapped_data_key"),
                            rs.getString("master_key_id"),
                            rs.getString("algorithm_id")
                    );
                    logger.log(Level.TRACE, "Found stored user key for user {0}", userId);
                    return Optional.of(storedKey);
                } else {
                    logger.log(Level.TRACE, "No stored user key found for user {0}", userId);
                    return Optional.empty(); // No key found
                }
            }
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to find wrapped user key for user " + userId, e);
            throw new KeyManagementException("Persistence error finding user key", e);
        }
    }

    // --- Wrapped DEK Methods ---
    @Override
    public void saveWrappedDek(StoredWrappedDek dek) {
        logger.log(Level.DEBUG, "Saving wrapped DEK with ID: {0} for owner {1} tenant {2}", dek.dekId(), dek.ownerUserId(), dek.tenantId());
        Objects.requireNonNull(dek, "StoredWrappedDek cannot be null");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_WRAPPED_DEK_SQL)) {

            pstmt.setObject(1, dek.dekId().value());         // dek_id (UUID)
            pstmt.setObject(2, dek.ownerUserId().value());    // owner_user_id (UUID)
            pstmt.setObject(3, dek.tenantId().value());       // tenant_id (UUID)
            pstmt.setBytes(4, dek.wrappedDek());              // wrapped_dek (VARBINARY)
            pstmt.setString(5, dek.ownerKeyId());             // owner_key_id (VARCHAR)
            pstmt.setString(6, dek.algorithmId());            // algorithm_id (VARCHAR)
            pstmt.setTimestamp(7, Timestamp.from(dek.createdAt())); // created_at (TIMESTAMP)

            pstmt.executeUpdate();
            logger.log(Level.TRACE, "Successfully saved DEK {0}", dek.dekId());

        } catch (SQLException e) {
            // Consider checking for specific SQLState (e.g., PK violation '23505' in H2/PostgreSQL)
            logger.log(Level.ERROR, "Failed to save wrapped DEK " + dek.dekId(), e);
            throw new KeyManagementException("Persistence error saving wrapped DEK", e);
        }
    }

    @Override
    public Optional<StoredWrappedDek> findWrappedDekById(DekId dekId, TenantId tenantId) {
        logger.log(Level.DEBUG, "Finding wrapped DEK by ID: {0} in tenant {1}", dekId, tenantId);
        Objects.requireNonNull(dekId, "dekId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_WRAPPED_DEK_BY_ID_SQL)) {

            pstmt.setObject(1, dekId.value());   // WHERE dek_id = ?
            pstmt.setObject(2, tenantId.value());// AND tenant_id = ?

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserId ownerUserId = new UserId(rs.getObject("owner_user_id", UUID.class));
                    // TenantId retrievedTenantId = new TenantId(rs.getObject("tenant_id", UUID.class)); // We already filtered by it
                    byte[] wrappedDekBytes = rs.getBytes("wrapped_dek");
                    String ownerKeyId = rs.getString("owner_key_id");
                    String algorithmId = rs.getString("algorithm_id");
                    Instant createdAt = rs.getTimestamp("created_at").toInstant();

                    StoredWrappedDek storedDek = new StoredWrappedDek(
                            dekId, ownerUserId, tenantId, wrappedDekBytes, ownerKeyId, algorithmId, createdAt
                    );
                    logger.log(Level.TRACE, "Found stored DEK by ID: {0}", dekId);
                    return Optional.of(storedDek);
                } else {
                    logger.log(Level.TRACE, "No stored DEK found for ID: {0} in tenant {1}", dekId, tenantId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to find wrapped DEK by ID " + dekId, e);
            throw new KeyManagementException("Persistence error finding wrapped DEK", e);
        }
    }


    // --- Share Grant Methods ---
    @Override
    public void saveShareGrant(TenantId tenantId, ShareGrantId grantId, ShareGrantDetails details, byte[] wrappedDekForGrantee, String granteeKeyId, String algorithmId) {
        logger.log(Level.DEBUG, "Saving share grant ID: {0} for DEK Ref: {1}", grantId, details.dataReference());
        Objects.requireNonNull(details, "details cannot be null");
        Objects.requireNonNull(grantId, "grantId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(wrappedDekForGrantee, "wrappedDekForGrantee cannot be null");
        Objects.requireNonNull(granteeKeyId, "granteeKeyId cannot be null");
        Objects.requireNonNull(algorithmId, "algorithmId cannot be null");

        // Validate DekId format in dataReference before attempting save
        try {
            DekId.fromString(details.dataReference());
        } catch (Exception e) {
            logger.log(Level.ERROR, "Invalid dataReference format for DekId: " + details.dataReference());
            throw new IllegalArgumentException("Cannot save grant: details.dataReference must be a valid DekId UUID string: " + details.dataReference());
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(MERGE_SHARE_GRANT_SQL)) {

            pstmt.setObject(1, grantId.value());              // share_grant_id (UUID)
            pstmt.setObject(2, tenantId.value());              // tenant_id (UUID)
            pstmt.setObject(3, details.ownerUserId().value()); // owner_user_id (UUID)
            pstmt.setString(4, details.dataReference());       // data_reference (VARCHAR - DekId string)
            pstmt.setString(5, details.granteeType().name());  // grantee_type (VARCHAR)
            pstmt.setString(6, details.granteeId());           // grantee_id (VARCHAR)
            pstmt.setBytes(7, wrappedDekForGrantee);           // encrypted_data_key (VARBINARY)
            pstmt.setString(8, granteeKeyId);                  // grantee_key_id (VARCHAR)
            pstmt.setString(9, algorithmId);                   // algorithm_id (VARCHAR)
            // Handle nullable expiration timestamp
            if (details.expiration() != null) {
                pstmt.setTimestamp(10, Timestamp.from(details.expiration())); // expiration_timestamp
            } else {
                pstmt.setNull(10, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            // Column 11 (created_timestamp) is handled by CURRENT_TIMESTAMP in SQL

            pstmt.executeUpdate();
            logger.log(Level.TRACE, "Saved/Updated share grant {0}", grantId);

        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to save share grant " + grantId, e);
            throw new KeyManagementException("Persistence error saving share grant", e);
        }
    }

    @Override
    public Optional<StoredShareGrant> findShareGrantById(TenantId tenantId, ShareGrantId grantId) {
        logger.log(Level.DEBUG, "Finding share grant by ID: {0} in tenant {1}", grantId, tenantId);
        Objects.requireNonNull(grantId, "grantId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_GRANT_BY_ID_SQL)) {

            pstmt.setObject(1, grantId.value());   // WHERE share_grant_id = ?
            pstmt.setObject(2, tenantId.value()); // AND tenant_id = ?

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    StoredShareGrant grant = mapResultSetToStoredShareGrant(rs, grantId, tenantId); // Use helper
                    logger.log(Level.TRACE, "Found share grant by ID: {0}", grantId);
                    return Optional.of(grant);
                } else {
                    logger.log(Level.TRACE, "No share grant found for ID: {0}", grantId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to find share grant by ID " + grantId, e);
            throw new KeyManagementException("Persistence error finding share grant by ID", e);
        } catch (IllegalArgumentException e) {
            // Catch mapping errors (e.g., invalid GranteeType)
            logger.log(Level.ERROR, "Failed to map share grant data for ID " + grantId, e);
            throw new KeyManagementException("Invalid grant data in storage for grant " + grantId, e);
        }
    }

    @Override
    public List<StoredShareGrant> findActiveShareGrantsForDek(TenantId tenantId, DekId dekId, UserId accessingUserId) {
        logger.log(Level.DEBUG, "Finding active USER grants for DEK {0}, user {1} in tenant {2}", dekId, accessingUserId, tenantId);
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(dekId, "dekId cannot be null");
        Objects.requireNonNull(accessingUserId, "accessingUserId cannot be null");

        List<StoredShareGrant> activeGrants = new ArrayList<>();
        // This query currently only checks for direct USER grants.
        // TODO: Extend if ROLE/TENANT grants need checking (would require different query/logic).

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_ACTIVE_GRANTS_FOR_DEK_SQL)) {

            pstmt.setObject(1, tenantId.value());                 // tenant_id = ?
            pstmt.setString(2, dekId.value().toString());         // data_reference = ? (DEK ID string)
            pstmt.setString(3, GranteeType.USER.name());          // grantee_type = 'USER'
            pstmt.setString(4, accessingUserId.value().toString());// grantee_id = ? (Accessing user's ID string)

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        ShareGrantId grantId = new ShareGrantId(rs.getObject("share_grant_id", UUID.class));
                        // TenantId is known from input parameter
                        StoredShareGrant grant = mapResultSetToStoredShareGrant(rs, grantId, tenantId); // Use helper
                        activeGrants.add(grant);
                        logger.log(Level.TRACE, "Found active grant: {0} for DEK {1}", grantId, dekId);
                    } catch (IllegalArgumentException | SQLException mappingEx) {
                        logger.log(Level.ERROR, "Failed to map row during active grant search for DEK " + dekId, mappingEx);
                        // Continue processing other rows
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to find active share grants for user " + accessingUserId + ", DEK " + dekId, e);
            throw new KeyManagementException("Persistence error finding active share grants", e);
        }

        logger.log(Level.DEBUG, "Found {0} active grant(s) for user {1}, DEK {2}", activeGrants.size(), accessingUserId, dekId);
        return activeGrants;
    }


    @Override
    public void deleteShareGrant(TenantId tenantId, ShareGrantId grantId) {
        logger.log(Level.DEBUG, "Deleting share grant by ID: {0} in tenant {1}", grantId, tenantId);
        Objects.requireNonNull(grantId, "grantId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_GRANT_BY_ID_SQL)) {

            pstmt.setObject(1, grantId.value());   // WHERE share_grant_id = ?
            pstmt.setObject(2, tenantId.value()); // AND tenant_id = ?

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                logger.log(Level.WARNING, "Delete operation affected 0 rows for grant {0}. Grant might not exist or already deleted.", grantId);
            } else {
                logger.log(Level.INFO, "Successfully deleted share grant {0}. Rows affected: {1}", grantId, affectedRows);
            }
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to delete share grant " + grantId, e);
            throw new KeyManagementException("Persistence error deleting share grant", e);
        }
    }

    @Override
    public List<StoredShareGrant> findExpiredShareGrants(Instant cutoffTimestamp) {
        logger.log(Level.DEBUG, "Finding expired share grants before: {0}", cutoffTimestamp);
        Objects.requireNonNull(cutoffTimestamp, "cutoffTimestamp cannot be null");

        List<StoredShareGrant> expiredGrants = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(FIND_EXPIRED_GRANTS_SQL)) {

            pstmt.setTimestamp(1, Timestamp.from(cutoffTimestamp)); // WHERE expiration_timestamp < ?

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        ShareGrantId grantId = new ShareGrantId(rs.getObject("share_grant_id", UUID.class));
                        TenantId tenantId = new TenantId(rs.getObject("tenant_id", UUID.class)); // Need tenantId for mapping
                        StoredShareGrant grant = mapResultSetToStoredShareGrant(rs, grantId, tenantId); // Use helper

                        // Extra check (though query should ensure it)
                        if (grant.expirationTimestamp() == null || !grant.expirationTimestamp().isBefore(cutoffTimestamp)) {
                            logger.log(Level.WARNING, "Expired grant query returned unexpected non-expired grant {0}", grantId);
                            continue;
                        }
                        expiredGrants.add(grant);
                        logger.log(Level.TRACE,"Found expired grant: {0}", grantId);
                    } catch (IllegalArgumentException | SQLException mappingEx) {
                        logger.log(Level.ERROR, "Failed to map row during expired grant search", mappingEx);
                        // Continue processing other rows
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Failed to find expired share grants before " + cutoffTimestamp, e);
            throw new KeyManagementException("Persistence error finding expired share grants", e);
        }

        logger.log(Level.DEBUG, "Found {0} expired grant(s) before {1}", expiredGrants.size(), cutoffTimestamp);
        return expiredGrants;
    }


    // --- Helper Methods ---

    /**
     * Maps a row from a ResultSet (expected to contain SHARE_GRANTS columns)
     * to a StoredShareGrant record. Assumes shareGrantId and tenantId are known.
     *
     * @param rs        The ResultSet positioned at the current row.
     * @param grantId   The ShareGrantId (already retrieved or known).
     * @param tenantId  The TenantId (already retrieved or known).
     * @return A StoredShareGrant instance.
     * @throws SQLException If a column is missing or has the wrong type.
     * @throws IllegalArgumentException If GranteeType string is invalid.
     */
    private StoredShareGrant mapResultSetToStoredShareGrant(ResultSet rs, ShareGrantId grantId, TenantId tenantId) throws SQLException, IllegalArgumentException {
        UserId ownerUserId = new UserId(rs.getObject("owner_user_id", UUID.class));
        String dataReference = rs.getString("data_reference"); // Should be DekId string
        GranteeType granteeType = GranteeType.valueOf(rs.getString("grantee_type")); // Throws IllegalArgumentException if invalid
        String granteeIdStr = rs.getString("grantee_id");
        byte[] encryptedDataKey = rs.getBytes("encrypted_data_key");
        String granteeKeyId = rs.getString("grantee_key_id");
        String algorithmId = rs.getString("algorithm_id");

        Timestamp expirationTs = rs.getTimestamp("expiration_timestamp");
        Instant expirationInstant = (expirationTs != null) ? expirationTs.toInstant() : null;

        Timestamp createdTs = rs.getTimestamp("created_timestamp");
        Instant createdInstant = (createdTs != null) ? createdTs.toInstant() : Instant.EPOCH; // Default if DB somehow allows null

        // Construct the record
        return new StoredShareGrant(
                grantId, tenantId, ownerUserId, dataReference,
                granteeType, granteeIdStr, encryptedDataKey, granteeKeyId,
                algorithmId, expirationInstant, createdInstant
        );
    }
}
// --- End File: src/main/java/com/ses/mylifeconduit/infrastructure/persistence/keys/JdbcKeyRepository.java ---
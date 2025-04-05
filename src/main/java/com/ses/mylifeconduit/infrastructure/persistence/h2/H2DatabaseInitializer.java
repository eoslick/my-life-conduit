// --- File: src/main/java/com/ses/mylifeconduit/infrastructure/persistence/h2/H2DatabaseInitializer.java ---
package com.ses.mylifeconduit.infrastructure.persistence.h2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
// --- Correct Logging Imports ---
import java.lang.System.Logger;
import java.lang.System.Logger.Level; // <<< Use this Level
// --- End Correct Logging Imports ---
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List; // Keep List import
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class to initialize an H2 database by executing SQL scripts from classpath resources.
 */
public final class H2DatabaseInitializer {

    private static final Logger logger = System.getLogger(H2DatabaseInitializer.class.getName());

    // JDBC URL constants
    public static final String DEFAULT_APP_DB_URL = "jdbc:h2:mem:mylifeconduit_app;DB_CLOSE_DELAY=-1";
    public static final String DEFAULT_KEYS_DB_URL = "jdbc:h2:mem:mylifeconduit_keys;DB_CLOSE_DELAY=-1";

    // Define lists of schema files in order
    private static final List<String> APP_SCHEMA_RESOURCES = List.of(
            "/db/migration/app/V1__create_app_schema.sql"
            // Add future app schema versions here: , "/db/migration/app/V2__..."
    );

    private static final List<String> KEYS_SCHEMA_RESOURCES = List.of(
            "/db/migration/keys/V1__create_keys_schema.sql",
            "/db/migration/keys/V2__create_wrapped_deks_table.sql"
            // Add future keys schema versions here
    );

    private H2DatabaseInitializer() { }

    /**
     * Initializes the H2 database at the given JDBC URL by executing a list of SQL scripts
     * found at the specified classpath resource paths, in the order provided.
     *
     * @param jdbcUrl            The JDBC URL for the H2 database.
     * @param schemaResourcePaths A list of classpath resource paths to the SQL schema files.
     * @throws IllegalStateException if any schema file cannot be loaded or if a database error occurs.
     */
    public static void initializeDatabase(String jdbcUrl, List<String> schemaResourcePaths) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
        Objects.requireNonNull(schemaResourcePaths, "schemaResourcePaths cannot be null");
        if (schemaResourcePaths.isEmpty()) {
            // Use the correct Level enum here
            logger.log(Level.WARNING, "No schema resource paths provided for URL: {0}", jdbcUrl);
            return;
        }

        // Use the correct Level enum here
        logger.log(Level.INFO, "Initializing H2 database schema from resources: {0} for URL: {1}", schemaResourcePaths, jdbcUrl);

        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            for (String schemaResourcePath : schemaResourcePaths) {
                // Use the correct Level enum here
                logger.log(Level.DEBUG, "Executing schema script: {0}", schemaResourcePath);
                String sqlScript;
                try {
                    sqlScript = loadResourceAsString(schemaResourcePath);
                } catch (IOException e) {
                    // Use the correct Level enum here
                    logger.log(Level.ERROR, "Failed to load schema resource: " + schemaResourcePath, e);
                    throw new IllegalStateException("Failed to load schema resource: " + schemaResourcePath, e);
                }

                try (Statement statement = connection.createStatement()) {
                    statement.execute(sqlScript);
                    // Use the correct Level enum here
                    logger.log(Level.DEBUG, "Successfully executed script: {0}", schemaResourcePath);
                } catch (SQLException e) {
                    // Use the correct Level enum here
                    logger.log(Level.ERROR, "Failed executing script: " + schemaResourcePath + " for URL: " + jdbcUrl, e);
                    logger.log(Level.ERROR, "SQLState: {0}, ErrorCode: {1}", e.getSQLState(), e.getErrorCode());
                    throw new IllegalStateException("Failed executing script: " + schemaResourcePath + " for URL: " + jdbcUrl, e);
                }
            }
            // Use the correct Level enum here
            logger.log(Level.INFO, "Database schema initialization complete for: {0}", jdbcUrl);
        } catch (SQLException e) {
            // Use the correct Level enum here
            logger.log(Level.ERROR, "Failed to establish connection or initialize database schema for URL: " + jdbcUrl, e);
            throw new IllegalStateException("Failed to establish connection or initialize database schema for URL: " + jdbcUrl, e);
        }
    }

    /**
     * Loads a classpath resource as a single String.
     *
     * @param resourcePath The path to the resource (e.g., "/db/schema.sql").
     * @return The content of the resource as a String.
     * @throws IOException If the resource cannot be found or read.
     */
    private static String loadResourceAsString(String resourcePath) throws IOException {
        InputStream inputStream = H2DatabaseInitializer.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Convenience method to initialize both the application and keys databases
     * using the default URLs and ordered schema paths.
     */
    public static void initializeDefaultDatabases() {
        // Use the correct Level enum here
        logger.log(Level.INFO, "Initializing default application and key databases...");
        initializeDatabase(DEFAULT_APP_DB_URL, APP_SCHEMA_RESOURCES);
        initializeDatabase(DEFAULT_KEYS_DB_URL, KEYS_SCHEMA_RESOURCES);
        // Use the correct Level enum here
        logger.log(Level.INFO, "Default databases initialization complete.");
    }
}
// --- End File: src/main/java/com/ses/mylifeconduit/infrastructure/persistence/h2/H2DatabaseInitializer.java ---
package com.ses.mylifeconduit;

import com.ses.mylifeconduit.infrastructure.persistence.h2.H2DatabaseInitializer;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class MainApplication {

    private static final Logger logger = System.getLogger(MainApplication.class.getName());

    public static void main(String[] args) {
        logger.log(Level.INFO, "Starting My Life Conduit application...");

        try {
            // Initialize H2 databases using SQL files from resources
            H2DatabaseInitializer.initializeDefaultDatabases();

            // --- Other application setup ---
            // Initialize services (Encryption, KMS, EventStore, Repositories...)
            // Setup Dependency Injection (if using a manual approach or framework)
            // Start API endpoints or UI
            // ---

            logger.log(Level.INFO, "Application setup complete. Ready.");
            // Keep the application running (e.g., if it's a server or UI app)
            // For a simple test, you might just let main exit here.

        } catch (Exception e) {
            logger.log(Level.ERROR, "Application failed to start.", e);
            System.exit(1); // Exit if critical setup fails
        }
    }
}
// --- File: src/main/java/com/ses/mylifeconduit/infrastructure/encryption/NoOpEncryptionService.java ---
package com.ses.mylifeconduit.infrastructure.encryption;

import com.ses.mylifeconduit.core.encryption.EncryptedValue;
import com.ses.mylifeconduit.core.encryption.EncryptionAlgorithm;
import com.ses.mylifeconduit.core.encryption.EncryptionService;
import com.ses.mylifeconduit.core.encryption.KeyContext; // KeyContext uses DekId now
import com.ses.mylifeconduit.core.security.exception.DecryptionException;
import com.ses.mylifeconduit.core.security.exception.EncryptionException;
// SecurityCoreException is not explicitly thrown here, but good to keep consistent imports if needed
// import com.ses.mylifeconduit.core.security.exception.SecurityCoreException;

import java.io.*;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * A non-encrypting implementation of {@link EncryptionService} for testing purposes ONLY.
 * <p>
 * WARNING: This service provides NO actual encryption or security. It simply passes through
 * the serialized data. DO NOT USE IN PRODUCTION ENVIRONMENTS.
 * <p>
 * It uses standard Java serialization to convert objects to bytes and back.
 */
public class NoOpEncryptionService implements EncryptionService {

    private static final Logger logger = System.getLogger(NoOpEncryptionService.class.getName());
    private static final String ALGORITHM_ID = EncryptionAlgorithm.NOOP;

    public NoOpEncryptionService() {
        logger.log(Level.WARNING, "*** NoOpEncryptionService is active. Data will NOT be encrypted. FOR TESTING ONLY. ***");
    }

    @Override
    public <T extends Serializable> EncryptedValue<T> encrypt(T plainValue, KeyContext context) {
        Objects.requireNonNull(plainValue, "plainValue cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        // <<< FIX: Use context.dekId() for logging >>>
        logger.log(Level.DEBUG, "NOOP 'Encrypting' data for DEK context: {0}", context.dekId());

        try {
            byte[] serializedData = serialize(plainValue);
            // Create EncryptedValue using the DekId's underlying UUID value for the keyContextId field
            // EncryptedValue still expects a UUID for keyContextId/dekContextId field
            return new EncryptedValue<>(serializedData, ALGORITHM_ID, context.dekId().value()); // <<< Use .value() here
        } catch (IOException e) {
            logger.log(Level.ERROR, "NOOP Encryption failed due to serialization error for context {0}", context.dekId(), e);
            throw new EncryptionException("NOOP Encryption failed: Could not serialize object", e);
        }
    }

    @Override
    public <T extends Serializable> T decrypt(EncryptedValue<T> encryptedValue) {
        Objects.requireNonNull(encryptedValue, "encryptedValue cannot be null");

        // The 'dekContextId' field in EncryptedValue is the UUID we need
        if (!ALGORITHM_ID.equals(encryptedValue.algorithmId())) {
            throw new DecryptionException("Algorithm mismatch: Expected " + ALGORITHM_ID +
                    " but received " + encryptedValue.algorithmId() +
                    " for key context " + encryptedValue.dekContextId()); // Use dekContextId() getter here
        }

        // Log using the dekContextId from the EncryptedValue
        logger.log(Level.DEBUG, "NOOP 'Decrypting' data for key context: {0}", encryptedValue.dekContextId());

        try {
            @SuppressWarnings("unchecked") // Necessary due to type erasure with serialization
            T deserializedObject = (T) deserialize(encryptedValue.encryptedData()); // Use encryptedData() getter
            return deserializedObject;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.ERROR, "NOOP Decryption failed due to deserialization error for context {0}", encryptedValue.dekContextId(), e);
            throw new DecryptionException("NOOP Decryption failed: Could not deserialize object", e);
        } catch (ClassCastException e) {
            logger.log(Level.ERROR, "NOOP Decryption failed due to type mismatch for context {0}", encryptedValue.dekContextId(), e);
            throw new DecryptionException("NOOP Decryption failed: Deserialized object is not of expected type", e);
        }
    }

    @Override
    public String getAlgorithmId() {
        return ALGORITHM_ID;
    }

    // --- Helper methods for standard Java Serialization ---

    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            return bos.toByteArray();
        }
    }

    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }
}
// --- End File: src/main/java/com/ses/mylifeconduit/infrastructure/encryption/NoOpEncryptionService.java ---
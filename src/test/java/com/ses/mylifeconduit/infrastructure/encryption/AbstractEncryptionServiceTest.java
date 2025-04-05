// --- File: src/test/java/com/ses/mylifeconduit/infrastructure/encryption/AbstractEncryptionServiceTest.java ---
package com.ses.mylifeconduit.infrastructure.encryption;

import com.ses.mylifeconduit.core.encryption.EncryptedValue;
import com.ses.mylifeconduit.core.encryption.EncryptionService;
import com.ses.mylifeconduit.core.encryption.KeyContext;
import com.ses.mylifeconduit.core.security.exception.DecryptionException;
import com.ses.mylifeconduit.core.security.exception.EncryptionException;
import com.ses.mylifeconduit.core.security.exception.SecurityCoreException;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serial; // Use java.io.Serial if Java 14+
import java.io.Serializable;
import java.util.UUID;

// --- This import line is the one causing the error ---
import com.ses.mylifeconduit.infrastructure.encryption.TestData;
// --- Ensure TestData.java exists at this location and has the correct package declaration ---

import static org.junit.jupiter.api.Assertions.*;

// ... (Rest of the class as provided previously) ...

public abstract class AbstractEncryptionServiceTest {

    protected EncryptionService encryptionService;
    protected TenantId testTenantId;
    protected UserId testUserId;

    protected abstract EncryptionService createEncryptionService();
    protected abstract String getExpectedAlgorithmId();

    @BeforeEach
    void setUp() {
        encryptionService = createEncryptionService();
        assertNotNull(encryptionService, "EncryptionService instance cannot be null");
        testTenantId = TenantId.generate();
        testUserId = UserId.generate();
    }

    @Test
    @DisplayName("Algorithm ID should match expected value for the implementation")
    void getAlgorithmId_shouldMatchExpected() {
        assertEquals(getExpectedAlgorithmId(), encryptionService.getAlgorithmId());
    }

    @Test
    @DisplayName("Encrypt/Decrypt cycle should return the original object")
    void encryptDecrypt_shouldReturnOriginalObject() {
        TestData originalData = new TestData("Testing " + getExpectedAlgorithmId(), 9876);
        KeyContext context = KeyContext.newContext(testTenantId, testUserId);
        EncryptedValue<TestData> encryptedValue = encryptionService.encrypt(originalData, context);
        TestData decryptedData = encryptionService.decrypt(encryptedValue);

        assertNotNull(encryptedValue);
        assertEquals(getExpectedAlgorithmId(), encryptedValue.algorithmId());
        assertEquals(context.dekId().value(), encryptedValue.dekContextId());
        assertNotNull(encryptedValue.encryptedData());
        assertTrue(encryptedValue.encryptedData().length > 0);
        assertNotNull(decryptedData);
        assertEquals(originalData, decryptedData);
        assertNotSame(originalData, decryptedData);
    }

    @Test
    @DisplayName("Decrypt should fail if algorithm ID does not match")
    void decrypt_shouldThrowException_whenAlgorithmIdMismatches() {
        byte[] dummyData = {1, 2, 3};
        UUID keyContextId = UUID.randomUUID();
        String wrongAlgorithmId = "WRONG_ALGO_" + UUID.randomUUID();
        if (wrongAlgorithmId.equals(getExpectedAlgorithmId())) {
            wrongAlgorithmId = "DEFINITELY_WRONG_ALGO";
        }
        EncryptedValue<TestData> mismatchValue = new EncryptedValue<>(dummyData, wrongAlgorithmId, keyContextId);

        DecryptionException exception = assertThrows(DecryptionException.class, () -> {
            encryptionService.decrypt(mismatchValue);
        });
        assertTrue(exception.getMessage().contains("Algorithm mismatch") || exception.getMessage().contains("Unsupported algorithm"));
    }

    @Test
    @DisplayName("Decrypt should fail gracefully for corrupted/invalid byte data")
    void decrypt_shouldThrowException_whenDataIsInvalid() {
        byte[] corruptedData = ("Invalid data " + getExpectedAlgorithmId()).getBytes();
        UUID keyContextId = UUID.randomUUID();
        EncryptedValue<TestData> corruptedValue = new EncryptedValue<>(corruptedData, getExpectedAlgorithmId(), keyContextId);

        SecurityCoreException exception = assertThrows(SecurityCoreException.class, () -> {
            encryptionService.decrypt(corruptedValue);
        });
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("Encrypt should fail for non-serializable field when using Java serialization")
    void encrypt_shouldFailForNonSerializableField_whenSerializationBased() {
        if (encryptionService instanceof NoOpEncryptionService) {
            class NonSerializableField { String data = "fail"; }
            class SerializableWrapper implements Serializable {
                @Serial private static final long serialVersionUID = 1L;
                private final NonSerializableField problematicField = new NonSerializableField();
                private final String otherData = "ok";
            }
            SerializableWrapper wrapper = new SerializableWrapper();
            KeyContext context = KeyContext.newContext(testTenantId, testUserId);

            EncryptionException exception = assertThrows(EncryptionException.class, () -> {
                encryptionService.encrypt(wrapper, context);
            });
            assertTrue(exception.getMessage().contains("Could not serialize object"));
            assertNotNull(exception.getCause());
            assertTrue(exception.getCause() instanceof java.io.NotSerializableException);
        } else {
            System.out.println("Skipping non-serializable field test for service: " + encryptionService.getClass().getSimpleName());
            assertTrue(true, "Test skipped");
        }
    }
}
// --- End File: src/test/java/com/ses/mylifeconduit/infrastructure/encryption/AbstractEncryptionServiceTest.java ---
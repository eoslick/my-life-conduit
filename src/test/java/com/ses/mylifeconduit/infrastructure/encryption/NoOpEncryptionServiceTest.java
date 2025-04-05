// src/test/java/com/ses/mylifeconduit/infrastructure/encryption/NoOpEncryptionServiceTest.java
package com.ses.mylifeconduit.infrastructure.encryption;

import com.ses.mylifeconduit.core.encryption.EncryptionAlgorithm;
import com.ses.mylifeconduit.core.encryption.EncryptionService;

/**
 * Concrete test class for {@link NoOpEncryptionService}.
 * Inherits all test logic from {@link AbstractEncryptionServiceTest}.
 */
class NoOpEncryptionServiceTest extends AbstractEncryptionServiceTest {

    @Override
    protected EncryptionService createEncryptionService() {
        // Provide the specific instance to test
        return new NoOpEncryptionService();
    }

    @Override
    protected String getExpectedAlgorithmId() {
        // Specify the expected algorithm ID for this implementation
        return EncryptionAlgorithm.NOOP;
    }

    // --- Add tests SPECIFIC to NoOpEncryptionService here, if any ---
    // For example, you might test that the 'encrypted' bytes actually correspond
    // directly to the Java serialized bytes of the original object.
}
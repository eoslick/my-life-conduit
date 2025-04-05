// --- File: src/main/java/com/ses/mylifeconduit/core/encryption/KeyContext.java ---
package com.ses.mylifeconduit.core.encryption;

import com.ses.mylifeconduit.core.security.keys.DekId; // <<< Import DekId
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents the contextual information needed to determine the appropriate
 * encryption/decryption key for an operation.
 * <p>
 * This context is typically passed to the {@link EncryptionService}. When encrypting,
 * the generated {@code dekId} is stored within the {@link EncryptedValue} metadata
 * (as keyContextId/retrievalContextId) and potentially used by the
 * {@link com.ses.mylifeconduit.core.security.KeyManagementService} to persist the wrapped DEK.
 *
 * @param tenantId The ID of the tenant owning the data or context.
 * @param userId   The ID of the user performing the operation or owning the key.
 * @param dekId    A unique identifier generated for this specific encryption operation,
 *                 representing the Data Encryption Key (DEK) used. <<< CHANGED TYPE & NAME
 *                 This ID will be stored in the {@link EncryptedValue} (as keyContextId)
 *                 and used later by the KMS to look up how the data was encrypted
 *                 (which DEK, which user key, which share grant etc).
 */
public record KeyContext(
        TenantId tenantId,
        UserId userId,
        DekId dekId // <<< CHANGED TYPE from UUID, RENAMED from contextId
) {
    /**
     * Canonical constructor validating inputs.
     */
    public KeyContext {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(dekId, "dekId cannot be null"); // <<< Use new field
    }

    /**
     * Factory method to create a new KeyContext with a generated DekId.
     *
     * @param tenantId The tenant ID.
     * @param userId   The user ID.
     * @return A new KeyContext instance.
     */
    public static KeyContext newContext(TenantId tenantId, UserId userId) {
        // Generate a new DekId for this context
        return new KeyContext(tenantId, userId, DekId.generate()); // <<< Use DekId.generate()
    }
}
// --- End File: src/main/java/com/ses/mylifeconduit/core/encryption/KeyContext.java ---
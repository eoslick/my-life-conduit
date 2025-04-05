package com.ses.mylifeconduit.core.security.keys;

import com.ses.mylifeconduit.core.ddd.ValueObject;
import com.ses.mylifeconduit.core.security.sharing.GranteeType;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the persisted information for a data sharing grant.
 * (Uses strongly-typed ShareGrantId).
 *
 * @param shareGrantId        Unique identifier for this sharing grant record. <<< CHANGED TYPE
 * @param tenantId            Tenant where the sharing occurs.
 * @param ownerUserId         User who owns the original data/key.
 * @param dataReference       Identifier for the shared data resource.
 * @param granteeType         Type: USER, ROLE, TENANT.
 * @param granteeId           Identifier of the grantee (UUID as String for User/Tenant, potentially role name).
 * @param encryptedDataKey    Owner's data key, re-encrypted using grantee's key/mechanism.
 * @param granteeKeyId        Identifier for the key/mechanism used to encrypt encryptedDataKey.
 * @param algorithmId         Algorithm used to encrypt encryptedDataKey.
 * @param expirationTimestamp Timestamp when the share expires (null for permanent).
 * @param createdTimestamp    Timestamp when the grant was stored.
 */
public record StoredShareGrant(
        ShareGrantId shareGrantId, // <<< CHANGED TYPE
        TenantId tenantId,
        UserId ownerUserId,
        String dataReference,
        GranteeType granteeType,
        String granteeId,
        byte[] encryptedDataKey,
        String granteeKeyId,
        String algorithmId,
        Instant expirationTimestamp, // Nullable
        Instant createdTimestamp
) implements ValueObject {

    public StoredShareGrant {
        // Required fields
        Objects.requireNonNull(shareGrantId, "shareGrantId cannot be null"); // <<< Validation uses new type
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        // ... rest of validations remain the same ...
        Objects.requireNonNull(createdTimestamp, "createdTimestamp cannot be null");

        // ... blank checks ...

        // Defensive copy for byte array
        encryptedDataKey = Arrays.copyOf(encryptedDataKey, encryptedDataKey.length);
    }

    // Getter for byte array remains the same
    public byte[] encryptedDataKey() {
        return Arrays.copyOf(encryptedDataKey, encryptedDataKey.length);
    }

    // equals, hashCode are updated automatically by the record for the new type
    // toString needs no change as ShareGrantId.toString() returns the UUID string

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredShareGrant that = (StoredShareGrant) o;
        return Objects.equals(shareGrantId, that.shareGrantId) && // Comparison uses ShareGrantId.equals()
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(ownerUserId, that.ownerUserId) &&
                Objects.equals(dataReference, that.dataReference) &&
                granteeType == that.granteeType &&
                Objects.equals(granteeId, that.granteeId) &&
                Arrays.equals(encryptedDataKey, that.encryptedDataKey) &&
                Objects.equals(granteeKeyId, that.granteeKeyId) &&
                Objects.equals(algorithmId, that.algorithmId) &&
                Objects.equals(expirationTimestamp, that.expirationTimestamp) &&
                Objects.equals(createdTimestamp, that.createdTimestamp);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(shareGrantId, tenantId, ownerUserId, dataReference, granteeType, granteeId, granteeKeyId, algorithmId, expirationTimestamp, createdTimestamp); // Hash uses ShareGrantId.hashCode()
        result = 31 * result + Arrays.hashCode(encryptedDataKey);
        return result;
    }

    @Override
    public String toString() {
        return "StoredShareGrant{" +
                "shareGrantId=" + shareGrantId + // Uses ShareGrantId.toString()
                ", tenantId=" + tenantId +
                ", ownerUserId=" + ownerUserId +
                // ... rest is the same ...
                '}';
    }
}
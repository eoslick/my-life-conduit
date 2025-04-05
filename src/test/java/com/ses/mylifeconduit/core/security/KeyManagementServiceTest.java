// --- File: src/test/java/com/ses/mylifeconduit/core/security/KeyManagementServiceTest.java ---
package com.ses.mylifeconduit.core.security;

import com.ses.mylifeconduit.core.security.exception.KeyManagementException;
import com.ses.mylifeconduit.core.security.keys.*; // Import all from keys
import com.ses.mylifeconduit.core.security.sharing.GranteeType;
import com.ses.mylifeconduit.core.security.sharing.ShareGrantDetails;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;
import com.ses.mylifeconduit.infrastructure.security.DefaultKeyManagementService; // Import the implementation

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList; // Import List
import java.util.List;    // Import List
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyManagementServiceTest {

    @Mock
    private KeyRepository keyRepository;

    // We test the implementation class directly here
    @InjectMocks
    private DefaultKeyManagementService keyManagementService;

    @Captor
    private ArgumentCaptor<ShareGrantId> grantIdCaptor;
    @Captor
    private ArgumentCaptor<ShareGrantDetails> detailsCaptor;
    @Captor
    private ArgumentCaptor<byte[]> wrappedKeyCaptor;
    @Captor
    private ArgumentCaptor<String> keyIdCaptor; // For granteeKeyId in saveShareGrant
    @Captor
    private ArgumentCaptor<String> algorithmIdCaptor; // For algorithmId in saveShareGrant

    // Test data
    private TenantId testTenantId;
    private UserId ownerUserId;
    private UserId granteeUserId;
    private String dekIdString; // Now store the DekId as string for dataReference
    private DekId dekId; // And the DekId object itself
    private StoredUserKey ownerStoredKey;
    private StoredUserKey granteeStoredKey;
    private StoredWrappedDek ownerStoredDek; // Needed for DEK resolution tests
    private byte[] dummyUserKeyData = "wrapped-user-key-data".getBytes();
    private byte[] dummyDekData = "wrapped-dek-data".getBytes();
    private String ownerMasterKeyId = "owner-master-key-01";
    private String granteeMasterKeyId = "grantee-master-key-01";
    private String ownerUserKeyAlgorithm = "AES_WRAP_USER"; // Example algo for user key
    private String dekWrapAlgorithm = "AES_WRAP_DEK"; // Example algo for DEK wrap

    @BeforeEach
    void setUp() {
        testTenantId = TenantId.generate();
        ownerUserId = UserId.generate();
        granteeUserId = UserId.generate();
        dekId = DekId.generate(); // Generate a DEK ID
        dekIdString = dekId.value().toString(); // Get its string representation

        ownerStoredKey = new StoredUserKey(dummyUserKeyData, ownerMasterKeyId, ownerUserKeyAlgorithm);
        granteeStoredKey = new StoredUserKey(dummyUserKeyData, granteeMasterKeyId, ownerUserKeyAlgorithm);

        // Create a stored DEK representation for the owner
        ownerStoredDek = new StoredWrappedDek(
                dekId, ownerUserId, testTenantId, dummyDekData,
                ownerMasterKeyId, // Assuming owner's master key ID is relevant here, adjust if needed
                dekWrapAlgorithm, Instant.now()
        );
    }

    // Helper to mock the user key unwrap process (returns a dummy key)
    private void mockUnwrapUserKey(UserId userIdToMock, StoredUserKey storedKeyToReturn) {
        when(keyRepository.findWrappedUserKey(eq(testTenantId), eq(userIdToMock)))
                .thenReturn(Optional.of(storedKeyToReturn));
        // No need to mock the actual crypto unwrap here, DefaultKMS stub handles it
    }

    // Helper to mock finding the owner's DEK
    private void mockFindOwnerDek(DekId dekIdToFind, StoredWrappedDek storedDekToReturn) {
        when(keyRepository.findWrappedDekById(eq(dekIdToFind), eq(testTenantId)))
                .thenReturn(Optional.of(storedDekToReturn));
    }

    @Test
    @DisplayName("createShareGrant should successfully retrieve keys, wrap DEK, and store grant")
    void createShareGrant_Success() {
        // Arrange
        ShareGrantDetails inputDetails = new ShareGrantDetails(
                ownerUserId, dekIdString, GranteeType.USER, // Use DekId string as dataReference
                granteeUserId.value().toString(), null, null // shareGrantId is null initially
        );

        // Mock finding owner's DEK (needed to get the DEK to wrap for grantee)
        mockFindOwnerDek(dekId, ownerStoredDek);
        // Mock unwrapping owner's USER key (needed to unwrap the DEK)
        mockUnwrapUserKey(ownerUserId, ownerStoredKey);
        // Mock unwrapping grantee's USER key (needed to wrap the DEK for the grantee)
        mockUnwrapUserKey(granteeUserId, granteeStoredKey);

        // Expect saveShareGrant call
        doNothing().when(keyRepository).saveShareGrant(
                any(TenantId.class), any(ShareGrantId.class), any(ShareGrantDetails.class),
                any(byte[].class), any(String.class), any(String.class)
        );

        // Act
        ShareGrantId generatedGrantId = keyManagementService.createShareGrant(testTenantId, inputDetails);

        // Assert
        assertNotNull(generatedGrantId, "Generated Grant ID should not be null");
        assertNotNull(generatedGrantId.value(), "Generated Grant ID value should not be null");

        // Verify repository interaction
        verify(keyRepository).saveShareGrant(
                eq(testTenantId),
                grantIdCaptor.capture(),
                detailsCaptor.capture(),
                wrappedKeyCaptor.capture(), // This is the DEK wrapped for the GRANTEE
                keyIdCaptor.capture(),      // This is the granteeKeyId used to wrap
                algorithmIdCaptor.capture() // This is the algorithm used to wrap
        );

        // Assert captured arguments
        assertEquals(generatedGrantId, grantIdCaptor.getValue());
        assertEquals(inputDetails, detailsCaptor.getValue()); // Ensure original details are passed
        assertNotNull(wrappedKeyCaptor.getValue());
        assertTrue(wrappedKeyCaptor.getValue().length > 0);
        // Assert granteeKeyId based on stub logic in DefaultKMS (uses grantee's masterKeyId)
        assertEquals(granteeMasterKeyId, keyIdCaptor.getValue());
        // Assert algorithm based on stub logic
        assertEquals("AES_WRAP", algorithmIdCaptor.getValue()); // DefaultKMS stub uses "AES_WRAP"

        // Verify key lookups performed
        verify(keyRepository, times(1)).findWrappedDekById(dekId, testTenantId); // Found owner DEK
        verify(keyRepository, times(1)).findWrappedUserKey(testTenantId, ownerUserId); // Unwrapped owner key
        verify(keyRepository, times(2)).findWrappedUserKey(testTenantId, granteeUserId); // Unwrapped grantee key
    }

    @Test
    @DisplayName("createShareGrant should fail if grantee user key is not found")
    void createShareGrant_Fail_GranteeKeyNotFound() {
        // Arrange
        ShareGrantDetails inputDetails = new ShareGrantDetails(
                ownerUserId, dekIdString, GranteeType.USER,
                granteeUserId.value().toString(), null, null
        );

        // Mock finding owner's DEK
        mockFindOwnerDek(dekId, ownerStoredDek);
        // Mock unwrapping owner's USER key
        mockUnwrapUserKey(ownerUserId, ownerStoredKey);
        // Mock Grantee Key *NOT* found in repository
        when(keyRepository.findWrappedUserKey(eq(testTenantId), eq(granteeUserId))).thenReturn(Optional.empty());

        // Act & Assert
        KeyManagementException exception = assertThrows(KeyManagementException.class, () -> {
            keyManagementService.createShareGrant(testTenantId, inputDetails);
        }, "Should throw KeyManagementException when grantee key is missing");

        assertTrue(exception.getMessage().contains("Grantee user key not found"), "Exception message should indicate grantee key not found");

        // Verify lookups occurred but save was not called
        verify(keyRepository, times(1)).findWrappedDekById(dekId, testTenantId);
        verify(keyRepository, times(1)).findWrappedUserKey(testTenantId, ownerUserId);
        verify(keyRepository, times(1)).findWrappedUserKey(testTenantId, granteeUserId);
        verify(keyRepository, never()).saveShareGrant(any(), any(), any(), any(), any(), any());
    }


    @Test
    @DisplayName("resolveDecryptionKey should return key when access is via valid, non-expired grant")
    void resolveDecryptionKey_Success_ViaGrant() {
        // Arrange
        ShareGrantId grantId = ShareGrantId.generate();
        UUID retrievalContextId = grantId.value(); // The ID passed for decryption IS the grant ID
        byte[] dekWrappedForGrantee = "dek-wrapped-for-grantee".getBytes();

        StoredShareGrant storedGrant = new StoredShareGrant(
                grantId, testTenantId, ownerUserId, dekIdString, // dataReference is the DekId string
                GranteeType.USER, granteeUserId.value().toString(), // Grantee is our user
                dekWrappedForGrantee, granteeMasterKeyId, // Key used to wrap the DEK for grantee
                dekWrapAlgorithm, // Algo used to wrap the DEK for grantee
                null, // Not expired
                Instant.now().minus(1, ChronoUnit.HOURS)
        );

        // Mock: Grant exists in repository when looked up by grantId
        when(keyRepository.findShareGrantById(eq(testTenantId), eq(grantId))).thenReturn(Optional.of(storedGrant));

        // Mock: Grantee's user key exists (needed to unwrap the DEK shared in the grant)
        mockUnwrapUserKey(granteeUserId, granteeStoredKey);

        // Act
        // <<< CALL THE CORRECT METHOD NAME >>>
        Optional<SecretKey> resolvedKeyOpt = keyManagementService.resolveDecryptionKey(testTenantId, granteeUserId, retrievalContextId);

        // Assert
        assertTrue(resolvedKeyOpt.isPresent(), "Should resolve a key via grant");
        assertNotNull(resolvedKeyOpt.get());
        assertEquals("AES", resolvedKeyOpt.get().getAlgorithm()); // Check dummy key algorithm from stub unwrap

        // Verify interactions
        verify(keyRepository, times(1)).findShareGrantById(testTenantId, grantId); // Found the grant
        verify(keyRepository, times(1)).findWrappedUserKey(testTenantId, granteeUserId); // Needed grantee key to unwrap DEK
        verify(keyRepository, never()).findWrappedDekById(any(DekId.class), any(TenantId.class)); // Did not need owner's DEK directly
    }

    @Test
    @DisplayName("resolveDecryptionKey should return empty Optional when user is not the grantee")
    void resolveDecryptionKey_Fail_NotGrantee() {
        // Arrange
        ShareGrantId grantId = ShareGrantId.generate();
        UUID retrievalContextId = grantId.value();
        UserId differentUserId = UserId.generate(); // A user who is NOT the grantee

        StoredShareGrant storedGrant = new StoredShareGrant(
                grantId, testTenantId, ownerUserId, dekIdString,
                GranteeType.USER, granteeUserId.value().toString(), // Grantee is granteeUserId
                "dummy-data".getBytes(), granteeMasterKeyId, dekWrapAlgorithm,
                null, Instant.now()
        );

        // Mock: Grant exists
        when(keyRepository.findShareGrantById(eq(testTenantId), eq(grantId))).thenReturn(Optional.of(storedGrant));

        // Act
        // <<< CALL THE CORRECT METHOD NAME >>>
        Optional<SecretKey> resolvedKeyOpt = keyManagementService.resolveDecryptionKey(testTenantId, differentUserId, retrievalContextId); // Trying to access as differentUser

        // Assert
        assertTrue(resolvedKeyOpt.isEmpty(), "Should return empty Optional for non-grantee");

        // Verify grant was checked, but no further key lookups were needed/performed for this user
        verify(keyRepository, times(1)).findShareGrantById(testTenantId, grantId);
        verify(keyRepository, never()).findWrappedUserKey(any(TenantId.class), any(UserId.class));
        verify(keyRepository, never()).findWrappedDekById(any(DekId.class), any(TenantId.class));
    }

    @Test
    @DisplayName("resolveDecryptionKey should return empty Optional when grant is expired")
    void resolveDecryptionKey_Fail_GrantExpired() {
        // Arrange
        ShareGrantId grantId = ShareGrantId.generate();
        UUID retrievalContextId = grantId.value();
        Instant pastExpiration = Instant.now().minus(1, ChronoUnit.DAYS); // Expired yesterday

        StoredShareGrant storedGrant = new StoredShareGrant(
                grantId, testTenantId, ownerUserId, dekIdString,
                GranteeType.USER, granteeUserId.value().toString(), // Grantee is our user
                "dummy-data".getBytes(), granteeMasterKeyId, dekWrapAlgorithm,
                pastExpiration, // Expired!
                Instant.now().minus(2, ChronoUnit.DAYS)
        );

        // Mock: Expired grant exists
        when(keyRepository.findShareGrantById(eq(testTenantId), eq(grantId))).thenReturn(Optional.of(storedGrant));

        // Act
        // <<< CALL THE CORRECT METHOD NAME >>>
        Optional<SecretKey> resolvedKeyOpt = keyManagementService.resolveDecryptionKey(testTenantId, granteeUserId, retrievalContextId); // Accessing as the correct grantee

        // Assert
        assertTrue(resolvedKeyOpt.isEmpty(), "Should return empty Optional for expired grant");

        // Verify grant was checked, but key lookup/unwrap was not performed
        verify(keyRepository, times(1)).findShareGrantById(testTenantId, grantId);
        verify(keyRepository, never()).findWrappedUserKey(any(), any());
        verify(keyRepository, never()).findWrappedDekById(any(), any());
    }

    @Test
    @DisplayName("resolveDecryptionKey should return key for owner when retrievalContextId is the DekId")
    void resolveDecryptionKey_Success_DirectOwnerAccess() {
        // Arrange
        UUID retrievalContextId = dekId.value(); // Context ID matches the owner's DEK ID

        // Mock: Grant lookup by this ID returns empty (it's not a grant ID)
        when(keyRepository.findShareGrantById(eq(testTenantId), eq(new ShareGrantId(retrievalContextId)))).thenReturn(Optional.empty());

        // Mock: Owner's DEK *is* found when looked up by DekId
        mockFindOwnerDek(dekId, ownerStoredDek);

        // Mock: Owner's user key exists (needed to unwrap the DEK)
        mockUnwrapUserKey(ownerUserId, ownerStoredKey);

        // Act
        // <<< CALL THE CORRECT METHOD NAME >>>
        Optional<SecretKey> resolvedKeyOpt = keyManagementService.resolveDecryptionKey(testTenantId, ownerUserId, retrievalContextId); // Owner accessing with DekId context

        // Assert
        assertTrue(resolvedKeyOpt.isPresent(), "Should resolve owner's key directly via DekId");
        assertNotNull(resolvedKeyOpt.get());
        assertEquals("AES", resolvedKeyOpt.get().getAlgorithm()); // From stub DEK unwrap

        // Verify interactions
        verify(keyRepository, times(1)).findShareGrantById(eq(testTenantId), eq(new ShareGrantId(retrievalContextId))); // Checked for grant first
        verify(keyRepository, times(1)).findWrappedDekById(eq(dekId), eq(testTenantId)); // Fallback lookup for DEK worked
        verify(keyRepository, times(1)).findWrappedUserKey(testTenantId, ownerUserId); // Needed owner key to unwrap DEK
    }

    @Test
    @DisplayName("resolveDecryptionKey should return empty Optional if context ID matches neither grant nor owned DEK")
    void resolveDecryptionKey_Fail_ContextIdNotFound() {
        // Arrange
        UUID unknownContextId = UUID.randomUUID(); // An ID that doesn't match anything known

        // Mock: Grant lookup returns empty
        when(keyRepository.findShareGrantById(eq(testTenantId), eq(new ShareGrantId(unknownContextId)))).thenReturn(Optional.empty());

        // Mock: DEK lookup by this ID also returns empty
        when(keyRepository.findWrappedDekById(eq(new DekId(unknownContextId)), eq(testTenantId))).thenReturn(Optional.empty());

        // Act
        // <<< CALL THE CORRECT METHOD NAME >>>
        Optional<SecretKey> resolvedKeyOpt = keyManagementService.resolveDecryptionKey(testTenantId, ownerUserId, unknownContextId);

        // Assert
        assertTrue(resolvedKeyOpt.isEmpty(), "Should return empty Optional if context ID is not found");

        // Verify interactions
        verify(keyRepository, times(1)).findShareGrantById(eq(testTenantId), eq(new ShareGrantId(unknownContextId))); // Checked grant
        verify(keyRepository, times(1)).findWrappedDekById(eq(new DekId(unknownContextId)), eq(testTenantId));      // Checked DEK
        verify(keyRepository, never()).findWrappedUserKey(any(), any()); // No key unwraps needed
    }

    @Test
    @DisplayName("revokeShareGrant should call deleteShareGrant on repository")
    void revokeShareGrant_Success() {
        // Arrange
        ShareGrantId grantId = ShareGrantId.generate();
        // UserId requestingUser = ownerUserId; // Add if authorization is tested

        // Mock: Expect deleteShareGrant call
        doNothing().when(keyRepository).deleteShareGrant(eq(testTenantId), eq(grantId));

        // Act
        assertDoesNotThrow(() -> { // Verify no exception is thrown
            // <<< CALL THE CORRECT METHOD NAME (revokeShareGrant was correct already) >>>
            keyManagementService.revokeShareGrant(testTenantId, grantId /*, requestingUser */);
        });

        // Verify interaction
        verify(keyRepository, times(1)).deleteShareGrant(testTenantId, grantId);
    }

}
// --- End File: src/test/java/com/ses/mylifeconduit/core/security/KeyManagementServiceTest.java ---
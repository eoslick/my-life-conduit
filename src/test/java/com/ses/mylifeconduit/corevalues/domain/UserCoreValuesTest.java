// --- File: com/ses/mylifeconduit/corevalues/domain/UserCoreValuesTest.java ---

package com.ses.mylifeconduit.corevalues.domain;

// Import Aggregate, VOs, Events, Exceptions from the domain
import com.ses.mylifeconduit.corevalues.domain.event.*;
import com.ses.mylifeconduit.corevalues.domain.exception.*;
import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueId;
import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueText;

// Import Core types
import com.ses.mylifeconduit.core.ddd.DomainEvent;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;

// Import testing framework classes (e.g., JUnit 5)
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// Import assertions
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant; // Import Instant
import java.util.List;
import java.util.Map;
import java.util.UUID; // Import UUID

/**
 * Unit tests for the UserCoreValues aggregate root.
 * Focuses on command validation and event generation.
 */
class UserCoreValuesTest {

    // Common test data
    private UserId testUserId;
    private TenantId testTenantId;
    private CoreValueId systemValueId1;
    private CoreValueText systemValueText1;
    private CoreValueId systemValueId2;
    private CoreValueText systemValueText2;
    private CoreValueId systemValueId3;
    private CoreValueText systemValueText3;
    private CoreValueId systemValueId4; // For testing limit
    private CoreValueText systemValueText4;


    @BeforeEach
    void setUp() {
        testUserId = UserId.generate();
        testTenantId = TenantId.generate();
        systemValueId1 = CoreValueId.generate(); // Simulate pre-existing system IDs
        systemValueText1 = new CoreValueText("Honesty");
        systemValueId2 = CoreValueId.generate();
        systemValueText2 = new CoreValueText("Kindness");
        systemValueId3 = CoreValueId.generate();
        systemValueText3 = new CoreValueText("Growth");
        systemValueId4 = CoreValueId.generate();
        systemValueText4 = new CoreValueText("Courage");
    }

    // Helper to get the first (and usually only) uncommitted event
    private <T extends DomainEvent> T assertSingleEventRaised(UserCoreValues aggregate, Class<T> eventType) {
        List<DomainEvent> events = aggregate.getUncommittedChanges();
        assertNotNull(events, "Event list should not be null");
        assertEquals(1, events.size(), "Expected exactly one event to be raised");
        DomainEvent event = events.get(0);
        assertInstanceOf(eventType, event, "Event type mismatch");
        return eventType.cast(event);
    }

    // Helper to assert no events were raised
    private void assertNoEventsRaised(UserCoreValues aggregate) {
        List<DomainEvent> events = aggregate.getUncommittedChanges();
        assertTrue(events.isEmpty(), "Expected no events to be raised");
    }

    // --- Initialization Tests ---

    @Test
    @DisplayName("initialize should create aggregate and raise UserCoreValuesInitialized event")
    void initialize_shouldRaiseInitializedEvent() {
        // Act
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);

        // Assert State
        assertNotNull(aggregate);
        assertEquals(testUserId, aggregate.getId());
        assertEquals(1, aggregate.getVersion()); // Version becomes 1 after first event

        // Assert Event
        UserCoreValuesInitialized event = assertSingleEventRaised(aggregate, UserCoreValuesInitialized.class);
        assertEquals(testUserId, event.aggregateId());
        assertEquals(testTenantId, event.tenantId());
        assertEquals(1L, event.aggregateVersion());

        aggregate.markChangesAsCommitted();
    }

    // --- Add Custom Value Tests ---

    @Test
    @DisplayName("addCustomCoreValue should raise CustomCoreValueAdded event")
    void addCustomCoreValue_Success() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.markChangesAsCommitted();
        CoreValueText customText = new CoreValueText("Integrity");

        // Act
        aggregate.addCustomCoreValue(customText, testTenantId);

        // Assert State
        assertEquals(2, aggregate.getVersion());

        // Assert Event
        CustomCoreValueAdded event = assertSingleEventRaised(aggregate, CustomCoreValueAdded.class);
        assertEquals(testUserId, event.aggregateId());
        assertEquals(testTenantId, event.tenantId());
        assertEquals(2L, event.aggregateVersion());
        assertNotNull(event.coreValueId());
        assertEquals(customText, event.coreValueText());

        // Assert internal state
        assertTrue(aggregate.getCustomValues().containsValue(customText));
        assertEquals(1, aggregate.getCustomValues().size());
    }

    @Test
    @DisplayName("addCustomCoreValue should throw exception for duplicate text (case-insensitive)")
    void addCustomCoreValue_DuplicateText_ThrowsException() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.addCustomCoreValue(new CoreValueText("Integrity"), testTenantId);
        aggregate.markChangesAsCommitted();
        CoreValueText duplicateText = new CoreValueText(" integrity ");

        // Act & Assert
        assertThrows(CustomValueTextAlreadyExistsException.class, () -> {
            aggregate.addCustomCoreValue(duplicateText, testTenantId);
        });
        assertNoEventsRaised(aggregate);
        assertEquals(1, aggregate.getCustomValues().size());
    }

    @Test
    @DisplayName("addCustomCoreValue should throw exception for invalid text")
    void addCustomCoreValue_InvalidText_ThrowsException() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.markChangesAsCommitted();

        // Act & Assert
        assertThrows(InvalidCoreValueTextException.class, () -> new CoreValueText(""));
        assertThrows(InvalidCoreValueTextException.class, () -> new CoreValueText("  "));
        assertThrows(InvalidCoreValueTextException.class, () -> new CoreValueText("A".repeat(101)));
        assertEquals(0, aggregate.getCustomValues().size());
    }

    // --- Select Core Value Tests ---

    @Test
    @DisplayName("selectCoreValue should raise CoreValueSelected event for system value")
    void selectCoreValue_SystemValue_Success() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.markChangesAsCommitted();

        // Act
        aggregate.selectCoreValue(systemValueId1, systemValueText1, true, testTenantId);

        // Assert
        assertEquals(2, aggregate.getVersion());
        CoreValueSelected event = assertSingleEventRaised(aggregate, CoreValueSelected.class);
        assertEquals(systemValueId1, event.coreValueId());
        assertEquals(systemValueText1, event.coreValueText());
        assertFalse(event.isCustom());
        assertEquals(1, aggregate.getSelectedValues().size());
        assertTrue(aggregate.getSelectedValues().containsKey(systemValueId1));
    }

    @Test
    @DisplayName("selectCoreValue should raise CoreValueSelected event for custom value")
    void selectCoreValue_CustomValue_Success() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        CoreValueText customText = new CoreValueText("Integrity");
        aggregate.addCustomCoreValue(customText, testTenantId);
        aggregate.markChangesAsCommitted();
        CoreValueId customId = aggregate.getCustomValues().entrySet().stream()
                .filter(entry -> entry.getValue().equals(customText))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();

        // Act
        aggregate.selectCoreValue(customId, customText, false, testTenantId);

        // Assert
        assertEquals(3, aggregate.getVersion());
        CoreValueSelected event = assertSingleEventRaised(aggregate, CoreValueSelected.class);
        assertEquals(customId, event.coreValueId());
        assertEquals(customText, event.coreValueText());
        assertTrue(event.isCustom());
        assertEquals(1, aggregate.getSelectedValues().size());
        assertTrue(aggregate.getSelectedValues().containsKey(customId));
    }

    @Test
    @DisplayName("selectCoreValue should allow selecting up to the limit")
    void selectCoreValue_UpToLimit() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.markChangesAsCommitted();

        // Act
        aggregate.selectCoreValue(systemValueId1, systemValueText1, true, testTenantId);
        aggregate.selectCoreValue(systemValueId2, systemValueText2, true, testTenantId);
        aggregate.selectCoreValue(systemValueId3, systemValueText3, true, testTenantId);
        aggregate.markChangesAsCommitted();

        // Assert state
        assertEquals(3, aggregate.getSelectedValues().size());
        assertEquals(4, aggregate.getVersion());
        assertNoEventsRaised(aggregate);
    }


    @Test
    @DisplayName("selectCoreValue should throw exception when limit is exceeded")
    void selectCoreValue_LimitExceeded_ThrowsException() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.selectCoreValue(systemValueId1, systemValueText1, true, testTenantId);
        aggregate.selectCoreValue(systemValueId2, systemValueText2, true, testTenantId);
        aggregate.selectCoreValue(systemValueId3, systemValueText3, true, testTenantId);
        aggregate.markChangesAsCommitted();

        // Act & Assert
        assertThrows(SelectionLimitExceededException.class, () -> {
            aggregate.selectCoreValue(systemValueId4, systemValueText4, true, testTenantId);
        });
        assertNoEventsRaised(aggregate);
        assertEquals(3, aggregate.getSelectedValues().size());
    }

    @Test
    @DisplayName("selectCoreValue should not raise event if value already selected")
    void selectCoreValue_AlreadySelected_NoEvent() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.selectCoreValue(systemValueId1, systemValueText1, true, testTenantId);
        aggregate.markChangesAsCommitted();

        // Act
        aggregate.selectCoreValue(systemValueId1, systemValueText1, true, testTenantId);

        // Assert
        assertNoEventsRaised(aggregate);
        assertEquals(1, aggregate.getSelectedValues().size());
        assertEquals(2, aggregate.getVersion());
    }

    @Test
    @DisplayName("selectCoreValue should throw exception for non-existent custom value")
    void selectCoreValue_CustomValueNotFound_ThrowsException() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.markChangesAsCommitted();
        CoreValueId nonExistentCustomId = CoreValueId.generate();
        CoreValueText someText = new CoreValueText("NonExistent");

        // Act & Assert
        assertThrows(ValueToSelectNotFoundException.class, () -> {
            aggregate.selectCoreValue(nonExistentCustomId, someText, false, testTenantId);
        });
        assertNoEventsRaised(aggregate);
        assertEquals(0, aggregate.getSelectedValues().size());
    }


    // --- Deselect Core Value Tests ---

    @Test
    @DisplayName("deselectCoreValue should raise CoreValueDeselected event")
    void deselectCoreValue_Success() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.selectCoreValue(systemValueId1, systemValueText1, true, testTenantId);
        aggregate.selectCoreValue(systemValueId2, systemValueText2, true, testTenantId);
        aggregate.markChangesAsCommitted();

        // Act
        aggregate.deselectCoreValue(systemValueId1, testTenantId);

        // Assert
        assertEquals(4, aggregate.getVersion());
        CoreValueDeselected event = assertSingleEventRaised(aggregate, CoreValueDeselected.class);
        assertEquals(systemValueId1, event.coreValueId());
        assertEquals(1, aggregate.getSelectedValues().size());
        assertFalse(aggregate.getSelectedValues().containsKey(systemValueId1));
        assertTrue(aggregate.getSelectedValues().containsKey(systemValueId2));
    }

    @Test
    @DisplayName("deselectCoreValue should throw exception when minimum selection is reached")
    void deselectCoreValue_MinimumReached_ThrowsException() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.selectCoreValue(systemValueId1, systemValueText1, true, testTenantId);
        aggregate.markChangesAsCommitted();

        // Act & Assert
        assertThrows(MinimumSelectionRequiredException.class, () -> {
            aggregate.deselectCoreValue(systemValueId1, testTenantId);
        });
        assertNoEventsRaised(aggregate);
        assertEquals(1, aggregate.getSelectedValues().size());
    }

    @Test
    @DisplayName("deselectCoreValue should throw exception if value not currently selected")
    void deselectCoreValue_ValueNotSelected_ThrowsException() {
        // Arrange
        UserCoreValues aggregate = UserCoreValues.initialize(testUserId, testTenantId);
        aggregate.selectCoreValue(systemValueId1, systemValueText1, true, testTenantId);
        aggregate.selectCoreValue(systemValueId2, systemValueText2, true, testTenantId);
        aggregate.markChangesAsCommitted();
        CoreValueId notSelectedId = CoreValueId.generate();

        // Act & Assert
        assertThrows(ValueNotSelectedException.class, () -> {
            aggregate.deselectCoreValue(notSelectedId, testTenantId);
        });
        assertNoEventsRaised(aggregate);
        assertEquals(2, aggregate.getSelectedValues().size());
    }

    // --- Optional: Test loading from history ---
    @Test
    @DisplayName("loadFromHistory should correctly restore aggregate state")
    void loadFromHistory_RestoresState() {
        // Arrange
        CoreValueText customText = new CoreValueText("Integrity");
        CoreValueId customId = CoreValueId.generate();
        Instant time1 = Instant.now();
        Instant time2 = time1.plusSeconds(1);
        Instant time3 = time2.plusSeconds(1);
        Instant time4 = time3.plusSeconds(1);

        List<DomainEvent> history = List.of(
                new UserCoreValuesInitialized(UUID.randomUUID(), testUserId, testTenantId, 1L, time1),
                new CustomCoreValueAdded(UUID.randomUUID(), testUserId, testTenantId, 2L, time2, customId, customText),
                new CoreValueSelected(UUID.randomUUID(), testUserId, testTenantId, 3L, time3, systemValueId1, systemValueText1, false), // isCustom = false
                new CoreValueSelected(UUID.randomUUID(), testUserId, testTenantId, 4L, time4, customId, customText, true) // isCustom = true
        );

        // Use the correct factory method for reconstitution
        UserCoreValues aggregate = UserCoreValues.forReconstitution(testUserId); // <<< CORRECTED CALL >>>

        // Act
        aggregate.loadFromHistory(history);

        // Assert Final State
        assertEquals(4, aggregate.getVersion());
        assertEquals(0, aggregate.getUncommittedChanges().size());
        assertEquals(1, aggregate.getCustomValues().size());
        assertTrue(aggregate.getCustomValues().containsKey(customId));
        assertEquals(customText, aggregate.getCustomValues().get(customId));
        assertEquals(2, aggregate.getSelectedValues().size());
        assertTrue(aggregate.getSelectedValues().containsKey(systemValueId1));
        assertEquals(systemValueText1, aggregate.getSelectedValues().get(systemValueId1));
        assertTrue(aggregate.getSelectedValues().containsKey(customId));
        assertEquals(customText, aggregate.getSelectedValues().get(customId));
    }
}
// --- End File ---
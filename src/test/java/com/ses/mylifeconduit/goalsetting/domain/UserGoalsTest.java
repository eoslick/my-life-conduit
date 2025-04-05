package com.ses.mylifeconduit.goalsetting.domain;

// Import Aggregate, VOs, Events, Exceptions from the domain
import com.ses.mylifeconduit.goalsetting.domain.event.*;
import com.ses.mylifeconduit.goalsetting.domain.exception.*;
import com.ses.mylifeconduit.goalsetting.domain.vo.*;

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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set; // Import Set
import java.util.UUID; // Import UUID

/**
 * Unit tests for the UserGoals aggregate root.
 */
class UserGoalsTest {

    // Common test data
    private UserId testUserId;
    private TenantId testTenantId;
    private GoalId longTermGoalId;
    private GoalId midTermGoalId;
    private GoalId secMidTermGoalId;
    private GoalId oneYearGoalId1;
    private GoalId oneYearGoalId2;
    private GoalId oneYearGoalId3;
    private GoalId oneYearGoalId4; // For limit testing
    private GoalDescription longTermDesc;
    private GoalDescription midTermDesc;
    private GoalDescription secMidTermDesc;
    private GoalDescription oneYearDesc1;
    private GoalDescription oneYearDesc2;
    private GoalDescription oneYearDesc3;
    private GoalDescription oneYearDesc4;
    private Circle testCircle;


    @BeforeEach
    void setUp() {
        testUserId = UserId.generate();
        testTenantId = TenantId.generate();
        testCircle = Circle.FINANCIAL;

        // Generate IDs
        longTermGoalId = GoalId.generate();
        midTermGoalId = GoalId.generate();
        secMidTermGoalId = GoalId.generate();
        oneYearGoalId1 = GoalId.generate();
        oneYearGoalId2 = GoalId.generate();
        oneYearGoalId3 = GoalId.generate();
        oneYearGoalId4 = GoalId.generate();

        // Create Descriptions
        longTermDesc = new GoalDescription("Achieve financial independence");
        midTermDesc = new GoalDescription("Build 1M net worth");
        secMidTermDesc = new GoalDescription("Save 500k");
        oneYearDesc1 = new GoalDescription("Save 100k this year");
        oneYearDesc2 = new GoalDescription("Increase income by 20%");
        oneYearDesc3 = new GoalDescription("Develop investment strategy");
        oneYearDesc4 = new GoalDescription("Track all expenses monthly");
    }

    // --- Helper Methods ---
    private <T extends DomainEvent> T assertSingleEventRaised(UserGoals aggregate, Class<T> eventType) {
        List<DomainEvent> events = aggregate.getUncommittedChanges();
        assertNotNull(events, "Event list should not be null");
        assertEquals(1, events.size(), "Expected exactly one event to be raised");
        DomainEvent event = events.get(0);
        assertInstanceOf(eventType, event, "Event type mismatch");
        return eventType.cast(event);
    }

    private void assertNoEventsRaised(UserGoals aggregate) {
        List<DomainEvent> events = aggregate.getUncommittedChanges();
        assertTrue(events.isEmpty(), "Expected no events to be raised");
    }

    private UserGoals createInitializedAggregate() {
        UserGoals aggregate = UserGoals.initialize(testUserId, testTenantId);
        aggregate.markChangesAsCommitted(); // Start clean after init
        return aggregate;
    }

    // Helper to perform standard breakdown for tests
    private void performStandardBreakdown(UserGoals aggregate) {
        aggregate.defineRootGoal(longTermGoalId, testCircle, Timeframe.LONG_TERM, longTermDesc, testTenantId);
        aggregate.breakdownGoal(midTermGoalId, longTermGoalId, Timeframe.MID_TERM, midTermDesc, testTenantId);
        aggregate.breakdownGoal(secMidTermGoalId, midTermGoalId, Timeframe.SECONDARY_MID_TERM, secMidTermDesc, testTenantId);
        aggregate.breakdownGoal(oneYearGoalId1, secMidTermGoalId, Timeframe.ONE_YEAR, oneYearDesc1, testTenantId);
        aggregate.markChangesAsCommitted();
    }

    // --- Initialization Tests ---
    @Test
    @DisplayName("initialize should create aggregate and raise UserGoalsInitialized event")
    void initialize_Success() {
        // Act
        UserGoals aggregate = UserGoals.initialize(testUserId, testTenantId);
        // Assert State
        assertEquals(testUserId, aggregate.getId());
        assertEquals(1, aggregate.getVersion());
        // Assert Event
        UserGoalsInitialized event = assertSingleEventRaised(aggregate, UserGoalsInitialized.class);
        assertEquals(testUserId, event.aggregateId());
        assertEquals(1L, event.aggregateVersion());
    }

    // --- Define Goal Tests ---
    @Test
    @DisplayName("defineRootGoal should raise GoalDefined event")
    void defineRootGoal_Success() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        // Act
        aggregate.defineRootGoal(longTermGoalId, testCircle, Timeframe.LONG_TERM, longTermDesc, testTenantId);
        // Assert State
        assertEquals(2, aggregate.getVersion()); // Init + Define
        assertTrue(aggregate.getAllGoalsDetails().containsKey(longTermGoalId));
        // Assert Event
        GoalDefined event = assertSingleEventRaised(aggregate, GoalDefined.class);
        assertEquals(longTermGoalId, event.goalId());
        assertEquals(testCircle, event.circle());
        assertEquals(Timeframe.LONG_TERM, event.timeframe());
        assertEquals(longTermDesc, event.description());
        assertNull(event.parentGoalId());
    }

    @Test
    @DisplayName("defineRootGoal should throw exception for duplicate ID")
    void defineRootGoal_DuplicateId_ThrowsException() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        aggregate.defineRootGoal(longTermGoalId, testCircle, Timeframe.LONG_TERM, longTermDesc, testTenantId);
        aggregate.markChangesAsCommitted();
        // Act & Assert
        GoalId duplicateId = longTermGoalId; // Use same ID
        assertThrows(GoalIdAlreadyExistsException.class, () -> {
            aggregate.defineRootGoal(duplicateId, Circle.PERSONAL, Timeframe.LONG_TERM, new GoalDescription("Different Goal"), testTenantId);
        });
        assertNoEventsRaised(aggregate);
    }

    // --- Breakdown Goal Tests ---
    @Test
    @DisplayName("breakdownGoal should raise GoalDefined event with parent ID")
    void breakdownGoal_Success() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        aggregate.defineRootGoal(longTermGoalId, testCircle, Timeframe.LONG_TERM, longTermDesc, testTenantId);
        aggregate.markChangesAsCommitted();
        // Act
        aggregate.breakdownGoal(midTermGoalId, longTermGoalId, Timeframe.MID_TERM, midTermDesc, testTenantId);
        // Assert State
        assertEquals(3, aggregate.getVersion()); // Init + DefineRoot + Breakdown
        assertTrue(aggregate.getAllGoalsDetails().containsKey(midTermGoalId));
        // Assert Event
        GoalDefined event = assertSingleEventRaised(aggregate, GoalDefined.class);
        assertEquals(midTermGoalId, event.goalId());
        assertEquals(testCircle, event.circle()); // Should inherit circle
        assertEquals(Timeframe.MID_TERM, event.timeframe());
        assertEquals(midTermDesc, event.description());
        assertEquals(longTermGoalId, event.parentGoalId()); // Parent ID set
    }

    @Test
    @DisplayName("breakdownGoal should throw exception for duplicate ID")
    void breakdownGoal_DuplicateId_ThrowsException() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        aggregate.defineRootGoal(longTermGoalId, testCircle, Timeframe.LONG_TERM, longTermDesc, testTenantId);
        aggregate.breakdownGoal(midTermGoalId, longTermGoalId, Timeframe.MID_TERM, midTermDesc, testTenantId);
        aggregate.markChangesAsCommitted();
        // Act & Assert
        GoalId duplicateId = midTermGoalId;
        assertThrows(GoalIdAlreadyExistsException.class, () -> {
            aggregate.breakdownGoal(duplicateId, longTermGoalId, Timeframe.MID_TERM, new GoalDescription("Different breakdown"), testTenantId);
        });
        assertNoEventsRaised(aggregate);
    }

    @Test
    @DisplayName("breakdownGoal should throw exception if parent goal not found")
    void breakdownGoal_ParentNotFound_ThrowsException() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        GoalId nonExistentParentId = GoalId.generate();
        // Act & Assert
        assertThrows(GoalNotFoundException.class, () -> {
            aggregate.breakdownGoal(midTermGoalId, nonExistentParentId, Timeframe.MID_TERM, midTermDesc, testTenantId);
        });
        assertNoEventsRaised(aggregate);
    }

    @Test
    @DisplayName("breakdownGoal should throw exception for invalid timeframe breakdown (e.g., LT to OY)")
    void breakdownGoal_InvalidTimeframe_ThrowsException() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        aggregate.defineRootGoal(longTermGoalId, testCircle, Timeframe.LONG_TERM, longTermDesc, testTenantId);
        aggregate.markChangesAsCommitted();
        // Act & Assert
        assertThrows(InvalidGoalBreakdownException.class, () -> {
            // Try breaking down LongTerm directly to OneYear
            aggregate.breakdownGoal(oneYearGoalId1, longTermGoalId, Timeframe.ONE_YEAR, oneYearDesc1, testTenantId);
        });
        assertNoEventsRaised(aggregate);
    }

    @Test
    @DisplayName("breakdownGoal should throw exception for breaking down ONE_YEAR goal")
    void breakdownGoal_FromOneYear_ThrowsException() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        performStandardBreakdown(aggregate); // Gets us to oneYearGoalId1
        // Act & Assert
        assertThrows(InvalidGoalBreakdownException.class, () -> {
            // Try breaking down the one-year goal
            aggregate.breakdownGoal(GoalId.generate(), oneYearGoalId1, Timeframe.ONE_YEAR, new GoalDescription("Sub-year task"), testTenantId);
        });
        assertNoEventsRaised(aggregate);
    }


    // --- Select Priority Goal Tests ---
    @Test
    @DisplayName("selectPriorityGoal should raise PriorityGoalSelected event")
    void selectPriorityGoal_Success() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        performStandardBreakdown(aggregate); // Defines oneYearGoalId1
        // Act
        aggregate.selectPriorityGoal(oneYearGoalId1, testTenantId);
        // Assert State
        assertTrue(aggregate.getPriorityGoalIds().contains(oneYearGoalId1));
        assertEquals(1, aggregate.getPriorityGoalIds().size());
        // Assert Event
        PriorityGoalSelected event = assertSingleEventRaised(aggregate, PriorityGoalSelected.class);
        assertEquals(oneYearGoalId1, event.goalId());
    }

    @Test
    @DisplayName("selectPriorityGoal should throw exception if goal not found")
    void selectPriorityGoal_GoalNotFound_ThrowsException() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        GoalId nonExistentGoalId = GoalId.generate();
        // Act & Assert
        assertThrows(GoalNotFoundException.class, () -> {
            aggregate.selectPriorityGoal(nonExistentGoalId, testTenantId);
        });
        assertNoEventsRaised(aggregate);
    }

    @Test
    @DisplayName("selectPriorityGoal should throw exception if goal is not ONE_YEAR")
    void selectPriorityGoal_NotOneYear_ThrowsException() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        performStandardBreakdown(aggregate);
        // Act & Assert
        assertThrows(InvalidGoalTimeframeException.class, () -> {
            aggregate.selectPriorityGoal(secMidTermGoalId, testTenantId); // Try selecting SecondaryMidTerm goal
        });
        assertNoEventsRaised(aggregate);
    }

    @Test
    @DisplayName("selectPriorityGoal should allow selecting up to limit")
    void selectPriorityGoal_UpToLimit() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        performStandardBreakdown(aggregate); // Creates oneYearGoalId1
        // Define two more one-year goals (assuming same parent for simplicity)
        aggregate.breakdownGoal(oneYearGoalId2, secMidTermGoalId, Timeframe.ONE_YEAR, oneYearDesc2, testTenantId);
        aggregate.breakdownGoal(oneYearGoalId3, secMidTermGoalId, Timeframe.ONE_YEAR, oneYearDesc3, testTenantId);
        aggregate.markChangesAsCommitted();
        // Act
        aggregate.selectPriorityGoal(oneYearGoalId1, testTenantId);
        aggregate.selectPriorityGoal(oneYearGoalId2, testTenantId);
        aggregate.selectPriorityGoal(oneYearGoalId3, testTenantId);
        // Assert State
        assertEquals(3, aggregate.getPriorityGoalIds().size());
    }

    @Test
    @DisplayName("selectPriorityGoal should throw exception when limit exceeded")
    void selectPriorityGoal_LimitExceeded_ThrowsException() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        performStandardBreakdown(aggregate); // Creates oneYearGoalId1
        aggregate.breakdownGoal(oneYearGoalId2, secMidTermGoalId, Timeframe.ONE_YEAR, oneYearDesc2, testTenantId);
        aggregate.breakdownGoal(oneYearGoalId3, secMidTermGoalId, Timeframe.ONE_YEAR, oneYearDesc3, testTenantId);
        aggregate.breakdownGoal(oneYearGoalId4, secMidTermGoalId, Timeframe.ONE_YEAR, oneYearDesc4, testTenantId);
        aggregate.selectPriorityGoal(oneYearGoalId1, testTenantId);
        aggregate.selectPriorityGoal(oneYearGoalId2, testTenantId);
        aggregate.selectPriorityGoal(oneYearGoalId3, testTenantId); // At limit
        aggregate.markChangesAsCommitted();
        // Act & Assert
        assertThrows(PriorityGoalLimitExceededException.class, () -> {
            aggregate.selectPriorityGoal(oneYearGoalId4, testTenantId); // Try selecting 4th
        });
        assertNoEventsRaised(aggregate);
        assertEquals(3, aggregate.getPriorityGoalIds().size());
    }

    @Test
    @DisplayName("selectPriorityGoal should be idempotent if goal already selected")
    void selectPriorityGoal_AlreadySelected_NoEvent() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        performStandardBreakdown(aggregate);
        aggregate.selectPriorityGoal(oneYearGoalId1, testTenantId); // Select once
        aggregate.markChangesAsCommitted();
        // Act
        aggregate.selectPriorityGoal(oneYearGoalId1, testTenantId); // Select again
        // Assert
        assertNoEventsRaised(aggregate);
        assertEquals(1, aggregate.getPriorityGoalIds().size());
    }

    // --- Deselect Priority Goal Tests ---
    @Test
    @DisplayName("deselectPriorityGoal should raise PriorityGoalDeselected event")
    void deselectPriorityGoal_Success() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        performStandardBreakdown(aggregate);
        aggregate.selectPriorityGoal(oneYearGoalId1, testTenantId);
        aggregate.markChangesAsCommitted();
        // Act
        aggregate.deselectPriorityGoal(oneYearGoalId1, testTenantId);
        // Assert State
        assertFalse(aggregate.getPriorityGoalIds().contains(oneYearGoalId1));
        assertEquals(0, aggregate.getPriorityGoalIds().size());
        // Assert Event
        PriorityGoalDeselected event = assertSingleEventRaised(aggregate, PriorityGoalDeselected.class);
        assertEquals(oneYearGoalId1, event.goalId());
    }

    @Test
    @DisplayName("deselectPriorityGoal should be idempotent if goal not selected")
    void deselectPriorityGoal_NotSelected_NoEvent() {
        // Arrange
        UserGoals aggregate = createInitializedAggregate();
        performStandardBreakdown(aggregate); // Goal exists but isn't priority
        aggregate.markChangesAsCommitted();
        // Act
        aggregate.deselectPriorityGoal(oneYearGoalId1, testTenantId);
        // Assert
        assertNoEventsRaised(aggregate);
        assertEquals(0, aggregate.getPriorityGoalIds().size());
    }

    // --- Load From History Test ---
    @Test
    @DisplayName("loadFromHistory should correctly restore aggregate state")
    void loadFromHistory_RestoresState() {
        // Arrange
        Instant time1 = Instant.now();
        Instant time2 = time1.plusSeconds(1);
        Instant time3 = time2.plusSeconds(1);
        Instant time4 = time3.plusSeconds(1);
        Instant time5 = time4.plusSeconds(1);

        List<DomainEvent> history = List.of(
                new UserGoalsInitialized(UUID.randomUUID(), testUserId, testTenantId, 1L, time1),
                new GoalDefined(UUID.randomUUID(), testUserId, testTenantId, 2L, time2, longTermGoalId, testCircle, Timeframe.LONG_TERM, longTermDesc, null),
                new GoalDefined(UUID.randomUUID(), testUserId, testTenantId, 3L, time3, midTermGoalId, testCircle, Timeframe.MID_TERM, midTermDesc, longTermGoalId),
                new GoalDefined(UUID.randomUUID(), testUserId, testTenantId, 4L, time4, oneYearGoalId1, testCircle, Timeframe.ONE_YEAR, oneYearDesc1, midTermGoalId), // Skipped SMT for brevity
                new PriorityGoalSelected(UUID.randomUUID(), testUserId, testTenantId, 5L, time5, oneYearGoalId1)
        );

        // Act
        UserGoals aggregate = UserGoals.forReconstitution(testUserId);
        aggregate.loadFromHistory(history);

        // Assert Final State
        assertEquals(5, aggregate.getVersion());
        assertEquals(0, aggregate.getUncommittedChanges().size());

        // Check goals map
        Map<GoalId, GoalDetails> goals = aggregate.getAllGoalsDetails();
        assertEquals(3, goals.size());
        assertTrue(goals.containsKey(longTermGoalId));
        assertTrue(goals.containsKey(midTermGoalId));
        assertTrue(goals.containsKey(oneYearGoalId1));
        assertEquals(longTermGoalId, goals.get(midTermGoalId).parentGoalId());
        assertEquals(midTermGoalId, goals.get(oneYearGoalId1).parentGoalId());
        assertEquals(testCircle, goals.get(oneYearGoalId1).circle());
        assertEquals(Timeframe.ONE_YEAR, goals.get(oneYearGoalId1).timeframe());

        // Check priorities set
        Set<GoalId> priorities = aggregate.getPriorityGoalIds();
        assertEquals(1, priorities.size());
        assertTrue(priorities.contains(oneYearGoalId1));
    }

}
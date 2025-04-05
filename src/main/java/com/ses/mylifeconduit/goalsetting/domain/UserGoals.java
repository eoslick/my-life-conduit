package com.ses.mylifeconduit.goalsetting.domain;

import com.ses.mylifeconduit.core.ddd.AggregateRoot;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;
import com.ses.mylifeconduit.goalsetting.domain.event.*;
import com.ses.mylifeconduit.goalsetting.domain.exception.*;
import com.ses.mylifeconduit.goalsetting.domain.vo.*;

import java.time.Instant;
import java.util.*;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Aggregate Root managing the collection of goals for a specific user.
 */
public class UserGoals extends AggregateRoot<UserId> {

    private static final Logger logger = System.getLogger(UserGoals.class.getName());
    private static final int MAX_PRIORITY_GOALS = 3;

    private Map<GoalId, GoalDetails> goals;
    private Set<GoalId> priorityGoalIds;
    private boolean initialized;

    private UserGoals(UserId userId) {
        super(userId);
        this.goals = new HashMap<>();
        this.priorityGoalIds = new HashSet<>();
        this.initialized = false;
    }

    public static UserGoals forReconstitution(UserId userId) {
        Objects.requireNonNull(userId, "userId cannot be null for reconstitution");
        return new UserGoals(userId);
    }

    public static UserGoals initialize(UserId userId, TenantId tenantId) {
        UserGoals aggregate = new UserGoals(userId);
        aggregate.raiseEvent(new UserGoalsInitialized(
                UUID.randomUUID(), userId, tenantId, 1L, Instant.now()
        ));
        return aggregate;
    }

    // --- Command Methods ---

    public void defineRootGoal(GoalId goalId, Circle circle, Timeframe timeframe, GoalDescription description, TenantId tenantId) {
        ensureInitialized();
        Objects.requireNonNull(goalId, "goalId cannot be null");
        Objects.requireNonNull(circle, "circle cannot be null");
        Objects.requireNonNull(timeframe, "timeframe cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        if (timeframe != Timeframe.LONG_TERM) {
            logger.log(Level.WARNING, "Defining root goal {0} with non-standard timeframe {1} for user {2}", goalId, timeframe, getId());
        }

        if (goals.containsKey(goalId)) {
            logger.log(Level.WARNING, "Attempted to define goal with existing ID {0} for user {1}", goalId, getId());
            // <<< FIX: Throw concrete exception >>>
            throw new GoalIdAlreadyExistsException(goalId);
        }

        raiseEvent(new GoalDefined(
                UUID.randomUUID(), getId(), tenantId, getVersion() + 1, Instant.now(),
                goalId, circle, timeframe, description, null
        ));
    }

    public void breakdownGoal(GoalId newGoalId, GoalId parentGoalId, Timeframe newTimeframe, GoalDescription description, TenantId tenantId) {
        ensureInitialized();
        Objects.requireNonNull(newGoalId, "newGoalId cannot be null");
        Objects.requireNonNull(parentGoalId, "parentGoalId cannot be null");
        Objects.requireNonNull(newTimeframe, "newTimeframe cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        GoalDetails parentGoal = goals.get(parentGoalId);
        if (parentGoal == null) {
            throw new GoalNotFoundException(parentGoalId);
        }

        if (goals.containsKey(newGoalId)) {
            // <<< FIX: Throw concrete exception >>>
            throw new GoalIdAlreadyExistsException(newGoalId);
        }

        validateTimeframeBreakdown(parentGoal.timeframe(), newTimeframe);
        Circle expectedCircle = parentGoal.circle();

        raiseEvent(new GoalDefined(
                UUID.randomUUID(), getId(), tenantId, getVersion() + 1, Instant.now(),
                newGoalId, expectedCircle, newTimeframe, description, parentGoalId
        ));
    }

    public void selectPriorityGoal(GoalId goalId, TenantId tenantId) {
        ensureInitialized();
        Objects.requireNonNull(goalId, "goalId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        GoalDetails goal = goals.get(goalId);
        if (goal == null) { throw new GoalNotFoundException(goalId); }
        if (goal.timeframe() != Timeframe.ONE_YEAR) { throw new InvalidGoalTimeframeException(Timeframe.ONE_YEAR); }
        if (priorityGoalIds.size() >= MAX_PRIORITY_GOALS && !priorityGoalIds.contains(goalId)) { // Check only if adding new one
            throw new PriorityGoalLimitExceededException(MAX_PRIORITY_GOALS);
        }
        if (priorityGoalIds.contains(goalId)) {
            logger.log(Level.WARNING, "Goal {0} is already a priority for user {1}. Ignoring select command.", goalId, getId());
            return;
        }

        raiseEvent(new PriorityGoalSelected(
                UUID.randomUUID(), getId(), tenantId, getVersion() + 1, Instant.now(), goalId
        ));
    }

    public void deselectPriorityGoal(GoalId goalId, TenantId tenantId) {
        ensureInitialized();
        Objects.requireNonNull(goalId, "goalId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        if (!priorityGoalIds.contains(goalId)) {
            logger.log(Level.WARNING, "Goal {0} is not a priority for user {1}. Ignoring deselect command.", goalId, getId());
            return;
        }

        raiseEvent(new PriorityGoalDeselected(
                UUID.randomUUID(), getId(), tenantId, getVersion() + 1, Instant.now(), goalId
        ));
    }


    // --- Event Application Methods (Mutate State) ---

    protected void apply(UserGoalsInitialized event) {
        this.initialized = true;
        if (this.goals == null) this.goals = new HashMap<>();
        if (this.priorityGoalIds == null) this.priorityGoalIds = new HashSet<>();
        logger.log(Level.TRACE, "Applied UserGoalsInitialized for user {0}", event.aggregateId());
    }

    protected void apply(GoalDefined event) {
        ensureInitialized();
        GoalDetails details = new GoalDetails( event.goalId(), event.circle(), event.timeframe(), event.description(), event.parentGoalId() );
        this.goals.put(event.goalId(), details);
        logger.log(Level.TRACE, "Applied GoalDefined: {0} ('{1}', {2}, {3}, Parent: {4}) for user {5}",
                event.goalId(), event.description().value(), event.circle(), event.timeframe(), event.parentGoalId(), event.aggregateId());
    }

    protected void apply(PriorityGoalSelected event) {
        ensureInitialized();
        this.priorityGoalIds.add(event.goalId());
        logger.log(Level.TRACE, "Applied PriorityGoalSelected: {0} for user {1}", event.goalId(), event.aggregateId());
    }

    protected void apply(PriorityGoalDeselected event) {
        ensureInitialized();
        this.priorityGoalIds.remove(event.goalId());
        logger.log(Level.TRACE, "Applied PriorityGoalDeselected: {0} for user {1}", event.goalId(), event.aggregateId());
    }

    // --- Helper Methods ---

    private void ensureInitialized() {
        if (!this.initialized) {
            throw new IllegalStateException("UserGoals aggregate for user " + getId() + " has not been initialized.");
        }
    }

    private void validateTimeframeBreakdown(Timeframe parentTimeframe, Timeframe newTimeframe) {
        boolean valid = switch (parentTimeframe) {
            case LONG_TERM -> newTimeframe == Timeframe.MID_TERM;
            case MID_TERM -> newTimeframe == Timeframe.SECONDARY_MID_TERM;
            case SECONDARY_MID_TERM -> newTimeframe == Timeframe.ONE_YEAR;
            case ONE_YEAR -> false;
        };
        if (!valid) {
            throw new InvalidGoalBreakdownException("Invalid timeframe breakdown: Cannot break down " + parentTimeframe + " to " + newTimeframe);
        }
    }

    // --- Getters for Querying State ---

    public Map<GoalId, GoalDetails> getAllGoalsDetails() {
        return Collections.unmodifiableMap(goals);
    }

    public Set<GoalId> getPriorityGoalIds() {
        return Collections.unmodifiableSet(priorityGoalIds);
    }

}
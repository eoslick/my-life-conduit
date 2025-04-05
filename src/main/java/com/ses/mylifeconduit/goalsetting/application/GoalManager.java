package com.ses.mylifeconduit.goalsetting.application;

// DTOs and App Exceptions
import com.ses.mylifeconduit.goalsetting.application.dto.GoalDetailsDTO;
import com.ses.mylifeconduit.goalsetting.application.dto.UserGoalsDTO;
import com.ses.mylifeconduit.goalsetting.application.exception.UserGoalsNotFoundException;

// Domain specific types
import com.ses.mylifeconduit.goalsetting.domain.UserGoals;
import com.ses.mylifeconduit.goalsetting.domain.UserGoalsRepository;
import com.ses.mylifeconduit.goalsetting.domain.exception.*; // Goal Setting domain exceptions
import com.ses.mylifeconduit.goalsetting.domain.vo.*;

// Core types
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application Service for managing Goal Setting use cases.
 * Orchestrates interactions between adapters (API) and the domain model (UserGoals aggregate).
 */
public class GoalManager {

    private static final Logger logger = System.getLogger(GoalManager.class.getName());

    private final UserGoalsRepository userGoalsRepository;

    /**
     * Constructor for dependency injection.
     *
     * @param userGoalsRepository Repository for accessing UserGoals aggregates.
     */
    public GoalManager(UserGoalsRepository userGoalsRepository) {
        this.userGoalsRepository = Objects.requireNonNull(userGoalsRepository, "userGoalsRepository cannot be null");
        logger.log(Level.INFO, "GoalManager initialized.");
    }

    /**
     * Use Case: Defines a new root (typically Long-Term) goal for a specific circle.
     *
     * @param actingUserId  The user performing the action.
     * @param tenantId      The tenant context.
     * @param circleName    The name of the Circle (e.g., "FINANCIAL").
     * @param timeframeName The name of the Timeframe (e.g., "LONG_TERM").
     * @param description   The text description of the goal.
     * @return The GoalId of the newly defined goal.
     * @throws GoalSettingException If domain rules are violated (e.g., duplicate ID internally).
     * @throws IllegalArgumentException If Circle or Timeframe names are invalid.
     * @throws InvalidGoalDescriptionException If description is invalid.
     */
    public GoalId defineRootGoal(UserId actingUserId, TenantId tenantId, String circleName, String timeframeName, String description) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(circleName, "circleName cannot be null");
        Objects.requireNonNull(timeframeName, "timeframeName cannot be null");
        // Let VO constructors handle description validation

        // 1. Create VOs from input strings
        Circle circle = Circle.fromString(circleName); // Throws IllegalArgumentException if invalid
        Timeframe timeframe = Timeframe.valueOf(timeframeName.toUpperCase()); // Throws IllegalArgumentException
        GoalDescription descriptionVO = new GoalDescription(description); // Throws InvalidGoalDescriptionException
        GoalId newGoalId = GoalId.generate(); // Generate a new unique ID for this goal

        logger.log(Level.DEBUG, "Use Case: Define Root Goal for user {0}, Circle: {1}, Timeframe: {2}, New ID: {3}",
                actingUserId, circleName, timeframeName, newGoalId);

        // 2. Load or Initialize Aggregate
        UserGoals aggregate = findOrCreateAggregate(actingUserId, tenantId);

        // 3. Execute Domain Command
        aggregate.defineRootGoal(newGoalId, circle, timeframe, descriptionVO, tenantId);

        // 4. Save Aggregate
        userGoalsRepository.save(aggregate, tenantId, actingUserId);
        logger.log(Level.INFO, "Use Case Success: Defined Root Goal {0} for user {1}", newGoalId, actingUserId);

        return newGoalId;
    }

    /**
     * Use Case: Defines a new goal as a breakdown of a parent goal.
     *
     * @param actingUserId      The user performing the action.
     * @param tenantId          The tenant context.
     * @param parentGoalIdString The ID of the goal being broken down.
     * @param newTimeframeName  The timeframe for the new breakdown goal (e.g., "MID_TERM").
     * @param newDescription    The description of the new breakdown goal.
     * @return The GoalId of the newly defined breakdown goal.
     * @throws UserGoalsNotFoundException If the user's aggregate doesn't exist (implies parent doesn't exist).
     * @throws GoalNotFoundException If the specified parentGoalId doesn't exist within the aggregate.
     * @throws InvalidGoalBreakdownException If the timeframe breakdown is invalid.
     * @throws GoalSettingException If other domain rules are violated.
     * @throws IllegalArgumentException If timeframe name or ID strings are invalid.
     * @throws InvalidGoalDescriptionException If description is invalid.
     */
    public GoalId breakdownGoal(UserId actingUserId, TenantId tenantId, String parentGoalIdString, String newTimeframeName, String newDescription) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(parentGoalIdString, "parentGoalIdString cannot be null");
        Objects.requireNonNull(newTimeframeName, "newTimeframeName cannot be null");
        // Let VO constructors handle description validation

        // 1. Create VOs
        GoalId parentGoalId = GoalId.fromString(parentGoalIdString);
        Timeframe newTimeframe = Timeframe.valueOf(newTimeframeName.toUpperCase());
        GoalDescription descriptionVO = new GoalDescription(newDescription);
        GoalId newGoalId = GoalId.generate(); // Generate ID for the breakdown goal

        logger.log(Level.DEBUG, "Use Case: Breakdown Goal for user {0}, Parent: {1}, New Timeframe: {2}, New ID: {3}",
                actingUserId, parentGoalIdString, newTimeframeName, newGoalId);

        // 2. Load Aggregate (Must exist for breakdown)
        UserGoals aggregate = userGoalsRepository.findById(actingUserId, tenantId)
                .orElseThrow(() -> new UserGoalsNotFoundException(actingUserId)); // Can't breakdown if no goals exist

        // 3. Execute Domain Command
        aggregate.breakdownGoal(newGoalId, parentGoalId, newTimeframe, descriptionVO, tenantId);

        // 4. Save Aggregate
        userGoalsRepository.save(aggregate, tenantId, actingUserId);
        logger.log(Level.INFO, "Use Case Success: Broke down Parent Goal {0} to New Goal {1} for user {2}",
                parentGoalIdString, newGoalId, actingUserId);

        return newGoalId;
    }

    /**
     * Use Case: Marks an existing ONE_YEAR goal as a priority.
     *
     * @param actingUserId    The user performing the action.
     * @param tenantId        The tenant context.
     * @param goalIdString    The ID of the ONE_YEAR goal to mark as priority.
     * @throws UserGoalsNotFoundException If the user's aggregate doesn't exist.
     * @throws GoalNotFoundException If the specified goalId doesn't exist.
     * @throws InvalidGoalTimeframeException If the goal is not a ONE_YEAR goal.
     * @throws PriorityGoalLimitExceededException If the priority limit is reached.
     * @throws IllegalArgumentException If goalIdString is not a valid UUID.
     */
    public void selectPriorityGoal(UserId actingUserId, TenantId tenantId, String goalIdString) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(goalIdString, "goalIdString cannot be null");

        GoalId goalId = GoalId.fromString(goalIdString);

        logger.log(Level.DEBUG, "Use Case: Select Priority Goal {0} for user {1}", goalIdString, actingUserId);

        // 1. Load Aggregate (Must exist)
        UserGoals aggregate = userGoalsRepository.findById(actingUserId, tenantId)
                .orElseThrow(() -> new UserGoalsNotFoundException(actingUserId));

        // 2. Execute Domain Command
        aggregate.selectPriorityGoal(goalId, tenantId);

        // 3. Save Aggregate
        userGoalsRepository.save(aggregate, tenantId, actingUserId);
        logger.log(Level.INFO, "Use Case Success: Selected Priority Goal {0} for user {1}", goalIdString, actingUserId);
    }

    /**
     * Use Case: Removes a goal from the set of priority goals.
     *
     * @param actingUserId    The user performing the action.
     * @param tenantId        The tenant context.
     * @param goalIdString    The ID of the goal to remove from priorities.
     * @throws UserGoalsNotFoundException If the user's aggregate doesn't exist.
     * @throws GoalNotPriorityException If the goal is not currently a priority (though aggregate might handle idempotently).
     * @throws IllegalArgumentException If goalIdString is not a valid UUID.
     */
    public void deselectPriorityGoal(UserId actingUserId, TenantId tenantId, String goalIdString) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(goalIdString, "goalIdString cannot be null");

        GoalId goalId = GoalId.fromString(goalIdString);

        logger.log(Level.DEBUG, "Use Case: Deselect Priority Goal {0} for user {1}", goalIdString, actingUserId);

        // 1. Load Aggregate (Must exist)
        UserGoals aggregate = userGoalsRepository.findById(actingUserId, tenantId)
                .orElseThrow(() -> new UserGoalsNotFoundException(actingUserId));

        // 2. Execute Domain Command
        aggregate.deselectPriorityGoal(goalId, tenantId); // Aggregate handles idempotency

        // 3. Save Aggregate (even if no event was raised due to idempotency, doesn't hurt)
        userGoalsRepository.save(aggregate, tenantId, actingUserId);
        logger.log(Level.INFO, "Use Case Success: Deselected Priority Goal {0} for user {1}", goalIdString, actingUserId);
    }

    /**
     * Use Case: Retrieves a user's defined goals and priorities.
     *
     * @param actingUserId The ID of the user whose goals are requested.
     * @param tenantId     The tenant context.
     * @return A DTO containing the user's goal structure. Returns representation with empty lists if none found.
     */
    public UserGoalsDTO getUserGoals(UserId actingUserId, TenantId tenantId) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        logger.log(Level.DEBUG, "Use Case: Get User Goals for user {0}", actingUserId);

        // 1. Load Aggregate
        Optional<UserGoals> aggregateOpt = userGoalsRepository.findById(actingUserId, tenantId);

        if (aggregateOpt.isEmpty()) {
            logger.log(Level.INFO, "Use Case Result: No goals aggregate found for user {0}", actingUserId);
            return UserGoalsDTO.notFound(actingUserId.value().toString());
        }

        // 2. Map Aggregate State to DTO
        UserGoals aggregate = aggregateOpt.get();
        UserGoalsDTO dto = mapAggregateToDTO(aggregate);

        logger.log(Level.INFO, "Use Case Success: Retrieved User Goals for user {0}", actingUserId);
        return dto;
    }

    // --- Helper Methods ---

    /** Finds an existing aggregate or initializes a new one. */
    private UserGoals findOrCreateAggregate(UserId userId, TenantId tenantId) {
        return userGoalsRepository.findById(userId, tenantId)
                .orElseGet(() -> {
                    logger.log(Level.INFO, "No existing UserGoals aggregate found for user {0}, initializing.", userId);
                    return UserGoals.initialize(userId, tenantId);
                });
    }

    /** Maps the state of the UserGoals aggregate to the UserGoalsDTO. */
    private UserGoalsDTO mapAggregateToDTO(UserGoals aggregate) {
        // Map all GoalDetails from the aggregate's internal map to GoalDetailsDTOs
        List<GoalDetailsDTO> allGoalDTOs = aggregate.getAllGoalsDetails().values().stream()
                .map(details -> new GoalDetailsDTO(
                        details.id().value().toString(),
                        details.circle().name(), // Use enum name or getDisplayName()
                        details.timeframe().name(),
                        details.description().value(),
                        details.parentGoalId() != null ? details.parentGoalId().value().toString() : null
                ))
                .collect(Collectors.toList());

        // Map the set of priority GoalIds to a set of GoalId strings
        Set<String> priorityIds = aggregate.getPriorityGoalIds().stream()
                .map(goalId -> goalId.value().toString())
                .collect(Collectors.toSet());

        return new UserGoalsDTO(
                aggregate.getId().value().toString(),
                allGoalDTOs,
                priorityIds
        );
    }
}
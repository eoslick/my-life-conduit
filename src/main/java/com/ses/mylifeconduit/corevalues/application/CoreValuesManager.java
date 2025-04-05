// --- File: com/ses/mylifeconduit/corevalues/application/CoreValuesManager.java ---

package com.ses.mylifeconduit.corevalues.application;

import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;
import com.ses.mylifeconduit.corevalues.application.dto.CoreValueDTO;
import com.ses.mylifeconduit.corevalues.application.dto.UserCoreValuesDTO;
import com.ses.mylifeconduit.corevalues.application.exception.CoreValuesNotFoundException;
import com.ses.mylifeconduit.corevalues.domain.UserCoreValues;
import com.ses.mylifeconduit.corevalues.domain.UserCoreValuesRepository;
import com.ses.mylifeconduit.corevalues.domain.exception.CoreValuesException; // Base domain exception
import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueId;
import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueText;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application Service for managing User Core Values use cases.
 * Orchestrates interactions between adapters (API) and the domain model.
 */
public class CoreValuesManager {

    private static final Logger logger = System.getLogger(CoreValuesManager.class.getName());

    private final UserCoreValuesRepository userCoreValuesRepository;
    // Optional: Inject system value validator/provider if needed
    // private final SystemValueProvider systemValueProvider;

    /**
     * Constructor for dependency injection.
     *
     * @param userCoreValuesRepository Repository for accessing UserCoreValues aggregates.
     */
    public CoreValuesManager(UserCoreValuesRepository userCoreValuesRepository) {
        this.userCoreValuesRepository = Objects.requireNonNull(userCoreValuesRepository, "userCoreValuesRepository cannot be null");
        logger.log(Level.INFO, "CoreValuesManager initialized.");
    }

    /**
     * Handles the use case of a user adding a new custom core value definition.
     *
     * @param actingUserId      The ID of the user performing the action.
     * @param tenantId          The tenant context.
     * @param customValueText   The text for the new custom value.
     * @throws CoreValuesException       If domain validation fails (e.g., duplicate text).
     * @throws InvalidCoreValueTextException If text format is invalid (from VO constructor).
     */
    public void addCustomValue(UserId actingUserId, TenantId tenantId, String customValueText) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        // Let CoreValueText constructor handle null/blank/length validation
        CoreValueText textVO = new CoreValueText(customValueText);

        logger.log(Level.DEBUG, "Use Case: Add Custom Value '{0}' for user {1}", customValueText, actingUserId);

        // 1. Load or Initialize Aggregate
        // Find existing or initialize a new one if it's the user's first interaction
        UserCoreValues aggregate = findOrCreateAggregate(actingUserId, tenantId);

        // 2. Execute Domain Command
        aggregate.addCustomCoreValue(textVO, tenantId); // Domain exceptions propagate

        // 3. Save Aggregate
        userCoreValuesRepository.save(aggregate, tenantId, actingUserId);
        logger.log(Level.INFO, "Use Case Success: Added Custom Value '{0}' for user {1}", customValueText, actingUserId);
    }

    /**
     * Handles the use case of a user selecting a core value.
     *
     * @param actingUserId      The ID of the user performing the action.
     * @param tenantId          The tenant context.
     * @param valueIdString     The string representation of the CoreValueId to select.
     * @param valueTextString   The text corresponding to the valueId (for event payload).
     * @param isSystemValue     Flag indicating if the ID refers to a system value or a user's custom value.
     * @throws CoreValuesException       If domain validation fails (e.g., limit exceeded, custom ID not found).
     * @throws IllegalArgumentException If valueIdString is not a valid UUID.
     * @throws InvalidCoreValueTextException If valueTextString format is invalid.
     * // Optional: @throws SystemValueNotFoundException If isSystemValue is true and ID is invalid (if validation happens here).
     */
    public void selectValue(UserId actingUserId, TenantId tenantId, String valueIdString, String valueTextString, boolean isSystemValue) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(valueIdString, "valueIdString cannot be null");
        Objects.requireNonNull(valueTextString, "valueTextString cannot be null");

        // Create VOs - these handle basic format validation
        CoreValueId idVO = CoreValueId.fromString(valueIdString);
        CoreValueText textVO = new CoreValueText(valueTextString);

        logger.log(Level.DEBUG, "Use Case: Select Value ID {0} ('{1}', System: {2}) for user {3}",
                valueIdString, valueTextString, isSystemValue, actingUserId);

        // Optional: Validate system ID exists if isSystemValue is true
        // if (isSystemValue) {
        //     if (!systemValueProvider.isValidSystemId(idVO)) {
        //         throw new SystemValueNotFoundException(idVO);
        //     }
        // }

        // 1. Load or Initialize Aggregate
        UserCoreValues aggregate = findOrCreateAggregate(actingUserId, tenantId);

        // 2. Execute Domain Command
        aggregate.selectCoreValue(idVO, textVO, isSystemValue, tenantId); // Domain exceptions propagate

        // 3. Save Aggregate
        userCoreValuesRepository.save(aggregate, tenantId, actingUserId);
        logger.log(Level.INFO, "Use Case Success: Selected Value ID {0} for user {1}", valueIdString, actingUserId);
    }

    /**
     * Handles the use case of a user deselecting a core value.
     *
     * @param actingUserId      The ID of the user performing the action.
     * @param tenantId          The tenant context.
     * @param valueIdString     The string representation of the CoreValueId to deselect.
     * @throws CoreValuesNotFoundException If the user's core values aggregate doesn't exist.
     * @throws CoreValuesException       If domain validation fails (e.g., minimum needed, value not selected).
     * @throws IllegalArgumentException If valueIdString is not a valid UUID.
     */
    public void deselectValue(UserId actingUserId, TenantId tenantId, String valueIdString) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(valueIdString, "valueIdString cannot be null");

        CoreValueId idVO = CoreValueId.fromString(valueIdString);

        logger.log(Level.DEBUG, "Use Case: Deselect Value ID {0} for user {1}", valueIdString, actingUserId);

        // 1. Load Aggregate (Must exist to deselect)
        UserCoreValues aggregate = userCoreValuesRepository.findById(actingUserId, tenantId)
                .orElseThrow(() -> new CoreValuesNotFoundException(actingUserId));

        // 2. Execute Domain Command
        aggregate.deselectCoreValue(idVO, tenantId); // Domain exceptions propagate

        // 3. Save Aggregate
        userCoreValuesRepository.save(aggregate, tenantId, actingUserId);
        logger.log(Level.INFO, "Use Case Success: Deselected Value ID {0} for user {1}", valueIdString, actingUserId);
    }

    /**
     * Handles the use case of retrieving a user's defined and selected core values.
     *
     * @param actingUserId The ID of the user whose values are requested.
     * @param tenantId     The tenant context.
     * @return A DTO containing the user's core value information.
     */
    public UserCoreValuesDTO getUserValues(UserId actingUserId, TenantId tenantId) {
        Objects.requireNonNull(actingUserId, "actingUserId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        logger.log(Level.DEBUG, "Use Case: Get User Values for user {0}", actingUserId);

        // 1. Load Aggregate
        Optional<UserCoreValues> aggregateOpt = userCoreValuesRepository.findById(actingUserId, tenantId);

        if (aggregateOpt.isEmpty()) {
            logger.log(Level.INFO, "Use Case Result: No core values found for user {0}", actingUserId);
            return UserCoreValuesDTO.notFound(actingUserId.value().toString());
        }

        // 2. Map Aggregate State to DTO
        UserCoreValues aggregate = aggregateOpt.get();
        UserCoreValuesDTO dto = mapAggregateToDTO(aggregate);

        logger.log(Level.INFO, "Use Case Success: Retrieved User Values for user {0}", actingUserId);
        return dto;
    }


    // --- Helper Methods ---

    /**
     * Finds an existing aggregate or initializes a new one.
     */
    private UserCoreValues findOrCreateAggregate(UserId userId, TenantId tenantId) {
        return userCoreValuesRepository.findById(userId, tenantId)
                .orElseGet(() -> {
                    logger.log(Level.INFO, "No existing aggregate found for user {0}, initializing.", userId);
                    // Initialize directly here. The save operation after the command will persist the init event.
                    return UserCoreValues.initialize(userId, tenantId);
                });
        // Note: If initialize raised an event that MUST be saved before other commands,
        // you might save immediately after initialize and then reload.
        // But usually, commands can follow initialize before the first save.
    }

    /**
     * Maps the state of the UserCoreValues aggregate to the UserCoreValuesDTO.
     */
    private UserCoreValuesDTO mapAggregateToDTO(UserCoreValues aggregate) {
        List<CoreValueDTO> customDefs = aggregate.getCustomValues().entrySet().stream()
                .map(entry -> new CoreValueDTO(entry.getKey().value().toString(), entry.getValue().value()))
                .collect(Collectors.toList());

        List<CoreValueDTO> selected = aggregate.getSelectedValues().entrySet().stream()
                .map(entry -> new CoreValueDTO(entry.getKey().value().toString(), entry.getValue().value()))
                .collect(Collectors.toList());

        return new UserCoreValuesDTO(
                aggregate.getId().value().toString(),
                customDefs,
                selected
        );
    }
}
// --- End File ---
// --- File: com/ses/mylifeconduit/corevalues/domain/UserCoreValues.java ---

package com.ses.mylifeconduit.corevalues.domain;

import com.ses.mylifeconduit.core.ddd.AggregateRoot;
import com.ses.mylifeconduit.core.user.UserId; // Assuming core user structure
import com.ses.mylifeconduit.core.tenant.TenantId; // Needed for events
import com.ses.mylifeconduit.corevalues.domain.event.*; // Import all domain events
import com.ses.mylifeconduit.corevalues.domain.exception.*; // Import all domain exceptions
import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueId;
import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueText;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root representing the set of Core Values defined and selected by a specific user.
 * Identified by the UserId.
 */
public class UserCoreValues extends AggregateRoot<UserId> {

    private static final int SELECTION_LIMIT = 3;
    private static final int MINIMUM_SELECTION = 1;

    // State
    private Map<CoreValueId, CoreValueText> customValues; // Custom values defined by this user
    private Map<CoreValueId, CoreValueText> selectedValues; // Currently selected values (system or custom)
    private boolean initialized; // Flag to track if aggregate is initialized

    /**
     * Factory method to create an aggregate instance specifically for
     * reconstitution from an event history. Does not raise initial event.
     *
     * @param userId The ID of the aggregate instance.
     * @return A new, empty UserCoreValues instance ready for history replay.
     */
    public static UserCoreValues forReconstitution(UserId userId) {
        Objects.requireNonNull(userId, "userId cannot be null for reconstitution");
        return new UserCoreValues(userId); // Call the private constructor
    }


    /**
     * Constructor for creating a new UserCoreValues instance.
     * Protected: Use the static factory method 'initialize' for creation.
     *
     * @param userId The ID of the user this aggregate belongs to.
     */
    private UserCoreValues(UserId userId) {
        super(userId); // Pass ID to AggregateRoot base class
        this.customValues = new HashMap<>();
        this.selectedValues = new HashMap<>();
        this.initialized = false;
    }

    /**
     * Factory method to initialize a new UserCoreValues aggregate for a user.
     *
     * @param userId   The ID of the user.
     * @param tenantId The tenant context.
     * @return A new UserCoreValues instance with an Initialization event raised.
     */
    public static UserCoreValues initialize(UserId userId, TenantId tenantId) {
        UserCoreValues userCoreValues = new UserCoreValues(userId);
        userCoreValues.raiseEvent(new UserCoreValuesInitialized(
                UUID.randomUUID(),          // eventId
                userId,                     // aggregateId (UserId)
                tenantId,                   // tenantId
                userCoreValues.getVersion() + 1, // next aggregateVersion
                Instant.now()               // occurredOn
        ));
        return userCoreValues;
    }

    // --- Command Methods ---

    /**
     * Adds a new custom core value definition for this user.
     *
     * @param coreValueText The text of the custom value.
     * @param tenantId      The tenant context for the event.
     * @throws CustomValueTextAlreadyExistsException If text already exists in custom values (case-insensitive check recommended).
     * @throws InvalidCoreValueTextException        If text is invalid (handled by CoreValueText constructor).
     */
    public void addCustomCoreValue(CoreValueText coreValueText, TenantId tenantId) {
        ensureInitialized();
        Objects.requireNonNull(coreValueText, "coreValueText cannot be null");

        // Check if text already exists (consider case-insensitive comparison)
        boolean textExists = customValues.values().stream()
                .anyMatch(existingText -> existingText.value().equalsIgnoreCase(coreValueText.value()));
        if (textExists) {
            throw new CustomValueTextAlreadyExistsException(coreValueText);
        }

        CoreValueId newId = CoreValueId.generate(); // Generate ID for the new custom value

        raiseEvent(new CustomCoreValueAdded(
                UUID.randomUUID(),
                getId(), // aggregateId
                tenantId,
                getVersion() + 1,
                Instant.now(),
                newId,
                coreValueText
        ));
    }

    /**
     * Selects a core value (either system or custom) for the user.
     *
     * @param coreValueId   The ID of the value to select.
     * @param coreValueText The text associated with the ID (provided for event payload).
     * @param isSystemValue True if the value is from the system list, false if it's from the user's custom list.
     * @param tenantId      The tenant context for the event.
     * @throws SelectionLimitExceededException If the selection limit is reached.
     * @throws ValueToSelectNotFoundException  If isSystemValue is false and the ID is not in the user's customValues.
     *                                         (Note: Checking existence of *system* values is typically an application layer concern before calling this command).
     */
    public void selectCoreValue(CoreValueId coreValueId, CoreValueText coreValueText, boolean isSystemValue, TenantId tenantId) {
        ensureInitialized();
        Objects.requireNonNull(coreValueId, "coreValueId cannot be null");
        Objects.requireNonNull(coreValueText, "coreValueText cannot be null");

        if (selectedValues.size() >= SELECTION_LIMIT) {
            throw new SelectionLimitExceededException(SELECTION_LIMIT);
        }

        if (selectedValues.containsKey(coreValueId)) {
            // Value already selected, maybe log or just ignore? For now, ignore.
            System.getLogger(UserCoreValues.class.getName()).log(System.Logger.Level.DEBUG,
                    "Value {0} already selected for user {1}. Ignoring select command.", coreValueId, getId());
            return;
        }

        // If it's a custom value, ensure it exists in the user's custom dictionary
        if (!isSystemValue && !customValues.containsKey(coreValueId)) {
            throw new ValueToSelectNotFoundException(coreValueId);
        }
        // Assumption: If isSystemValue is true, the application layer has already validated its existence.

        raiseEvent(new CoreValueSelected(
                UUID.randomUUID(),
                getId(),
                tenantId,
                getVersion() + 1,
                Instant.now(),
                coreValueId,
                coreValueText,
                !isSystemValue // isCustom flag is the opposite of isSystemValue
        ));
    }

    /**
     * Deselects a currently selected core value.
     *
     * @param coreValueId The ID of the value to deselect.
     * @param tenantId    The tenant context for the event.
     * @throws MinimumSelectionRequiredException If deselection would drop below the minimum required.
     * @throws ValueNotSelectedException         If the specified value is not currently selected.
     */
    public void deselectCoreValue(CoreValueId coreValueId, TenantId tenantId) {
        ensureInitialized();
        Objects.requireNonNull(coreValueId, "coreValueId cannot be null");

        if (!selectedValues.containsKey(coreValueId)) {
            throw new ValueNotSelectedException(coreValueId);
        }

        if (selectedValues.size() <= MINIMUM_SELECTION) {
            throw new MinimumSelectionRequiredException(MINIMUM_SELECTION);
        }

        raiseEvent(new CoreValueDeselected(
                UUID.randomUUID(),
                getId(),
                tenantId,
                getVersion() + 1,
                Instant.now(),
                coreValueId
        ));
    }

    // --- Event Application Methods (Mutate State) ---
    // These methods are called internally by AggregateRoot.applyChange

    protected void apply(UserCoreValuesInitialized event) {
        this.initialized = true;
        // Initialize maps if they weren't already (though constructor does this)
        if (this.customValues == null) this.customValues = new HashMap<>();
        if (this.selectedValues == null) this.selectedValues = new HashMap<>();
        System.getLogger(UserCoreValues.class.getName()).log(System.Logger.Level.DEBUG,
                "Applied UserCoreValuesInitialized for user {0}", event.aggregateId());
    }

    protected void apply(CustomCoreValueAdded event) {
        ensureInitialized(); // Should already be true if events applied correctly
        this.customValues.put(event.coreValueId(), event.coreValueText());
        System.getLogger(UserCoreValues.class.getName()).log(System.Logger.Level.DEBUG,
                "Applied CustomCoreValueAdded: {0} = '{1}' for user {2}", event.coreValueId(), event.coreValueText().value(), event.aggregateId());
    }

    protected void apply(CoreValueSelected event) {
        ensureInitialized();
        this.selectedValues.put(event.coreValueId(), event.coreValueText());
        System.getLogger(UserCoreValues.class.getName()).log(System.Logger.Level.DEBUG,
                "Applied CoreValueSelected: {0} = '{1}' (Custom: {2}) for user {3}",
                event.coreValueId(), event.coreValueText().value(), event.isCustom(), event.aggregateId());
    }

    protected void apply(CoreValueDeselected event) {
        ensureInitialized();
        this.selectedValues.remove(event.coreValueId());
        System.getLogger(UserCoreValues.class.getName()).log(System.Logger.Level.DEBUG,
                "Applied CoreValueDeselected: {0} for user {1}", event.coreValueId(), event.aggregateId());
    }

    // --- Helper Methods ---

    private void ensureInitialized() {
        if (!this.initialized) {
            // This indicates a logic error - commands should not be processed before initialization event.
            throw new IllegalStateException("UserCoreValues aggregate for user " + getId() + " has not been initialized.");
        }
    }

    // --- Getters for Querying State (optional, use carefully) ---
    // Primarily for testing or if Read Models aren't immediately available. Avoid using for command decisions.

    /**
     * Gets an unmodifiable view of the currently selected core values.
     * Use primarily for testing or read-only purposes.
     * @return Unmodifiable map of selected CoreValueId to CoreValueText.
     */
    public Map<CoreValueId, CoreValueText> getSelectedValues() {
        return Collections.unmodifiableMap(selectedValues);
    }

    /**
     * Gets an unmodifiable view of the custom core values defined by this user.
     * Use primarily for testing or read-only purposes.
     * @return Unmodifiable map of custom CoreValueId to CoreValueText.
     */
    public Map<CoreValueId, CoreValueText> getCustomValues() {
        return Collections.unmodifiableMap(customValues);
    }
}
// --- End File ---
package com.ses.mylifeconduit.corevalues.application.dto;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DTO representing a user's complete set of core values (custom definitions and selections).
 */
public record UserCoreValuesDTO(
        String userId, // String representation of UserId (UUID)
        List<CoreValueDTO> customValueDefinitions, // All custom values defined by the user
        List<CoreValueDTO> selectedValues // The values currently selected (1-3)
) {
    /**
     * Canonical constructor ensuring lists are not null.
     */
    public UserCoreValuesDTO {
        Objects.requireNonNull(userId, "userId cannot be null");
        // Ensure lists are unmodifiable and never null
        customValueDefinitions = (customValueDefinitions != null) ? List.copyOf(customValueDefinitions) : List.of();
        selectedValues = (selectedValues != null) ? List.copyOf(selectedValues) : List.of();
    }

    /**
     * Convenience factory for when user values are not found.
     */
    public static UserCoreValuesDTO notFound(String userId) {
        return new UserCoreValuesDTO(userId, List.of(), List.of());
    }
}
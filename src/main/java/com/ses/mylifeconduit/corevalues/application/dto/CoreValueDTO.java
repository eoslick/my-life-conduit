package com.ses.mylifeconduit.corevalues.application.dto;

import java.util.Objects;

/**
 * DTO representing a single Core Value (ID and Text).
 */
public record CoreValueDTO(
        String id, // String representation of CoreValueId (UUID)
        String text
) {
    public CoreValueDTO {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(text, "text cannot be null");
    }
}
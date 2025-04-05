package com.ses.mylifeconduit.goalsetting.domain.vo;

import com.ses.mylifeconduit.core.ddd.ValueObject;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents one of the seven key life areas (Circles) for goal setting.
 * Implemented as an Enum for type safety and restricted values.
 * Implements ValueObject marker interface.
 */
public enum Circle implements ValueObject {
    SPIRITUAL("Spiritual"),
    PHYSICAL("Physical"),
    PERSONAL("Personal"),
    KEY_RELATIONSHIPS("Key Relationships"),
    BUSINESS("Business"),
    JOB("Job"),
    FINANCIAL("Financial");

    private final String displayName;

    Circle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Finds a Circle by its case-insensitive name or display name.
     * @param name The name to search for.
     * @return The matching Circle enum constant.
     * @throws IllegalArgumentException if no matching Circle is found.
     */
    public static Circle fromString(String name) {
        Objects.requireNonNull(name, "Circle name cannot be null");
        String searchName = name.trim();
        return Arrays.stream(Circle.values())
                .filter(c -> c.name().equalsIgnoreCase(searchName) || c.displayName.equalsIgnoreCase(searchName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Circle: " + name));
    }

    @Override
    public String toString() {
        return displayName; // Default toString returns display name
    }
}
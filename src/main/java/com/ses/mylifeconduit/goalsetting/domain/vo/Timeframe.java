package com.ses.mylifeconduit.goalsetting.domain.vo;

import com.ses.mylifeconduit.core.ddd.ValueObject;

/**
 * Represents the different timeframes used in the 'Goal Setting to the Now' process.
 * Implemented as an Enum for type safety.
 * Implements ValueObject marker interface.
 */
public enum Timeframe implements ValueObject {
    LONG_TERM,       // e.g., 10+ years
    MID_TERM,        // e.g., floor(LT / 2)
    SECONDARY_MID_TERM, // e.g., floor(MT / 2)
    ONE_YEAR,        // The focus for 4-1-1 and Priority Goals
    // Potentially MONTHLY, WEEKLY if needed directly in this context, but likely belong to PlanningContext
}
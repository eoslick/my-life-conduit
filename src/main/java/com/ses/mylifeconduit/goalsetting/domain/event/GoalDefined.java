package com.ses.mylifeconduit.goalsetting.domain.event;

import com.ses.mylifeconduit.core.ddd.DomainEvent;
import com.ses.mylifeconduit.core.tenant.TenantId;
import com.ses.mylifeconduit.core.user.UserId;
import com.ses.mylifeconduit.goalsetting.domain.vo.*; // Import VOs

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Event indicating a new goal (root or breakdown) was defined for the user.
 */
public record GoalDefined(
        UUID eventId,
        UserId aggregateId,
        TenantId tenantId,
        long aggregateVersion,
        Instant occurredOn,
        // Event specific payload:
        GoalId goalId,
        Circle circle,
        Timeframe timeframe,
        GoalDescription description,
        GoalId parentGoalId // Nullable: Will be null for root goals
) implements DomainEvent {

    public static final String EVENT_VERSION = "V1";

    /** Canonical constructor validating inputs. */
    public GoalDefined {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(aggregateId, "aggregateId cannot be null");
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        if (aggregateVersion <= 0) throw new IllegalArgumentException("aggregateVersion must be positive");
        Objects.requireNonNull(occurredOn, "occurredOn cannot be null");
        Objects.requireNonNull(goalId, "goalId cannot be null");
        Objects.requireNonNull(circle, "circle cannot be null");
        Objects.requireNonNull(timeframe, "timeframe cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        // parentGoalId can be null
    }

    @Override
    public String eventVersion() {
        return EVENT_VERSION;
    }
    // equals, hashCode, toString are generated by the record
}
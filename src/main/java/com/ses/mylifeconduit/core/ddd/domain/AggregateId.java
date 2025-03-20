package com.ses.mylifeconduit.core.ddd.domain;

import java.util.UUID;

/**
 * Base record for all aggregate identifiers.
 */
public record AggregateId(UUID value) {}
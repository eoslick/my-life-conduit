-- Schema for the main application event store and audit log (Version 1)

-- EVENT_STORE Table
CREATE TABLE IF NOT EXISTS EVENT_STORE (
                                           event_id UUID PRIMARY KEY,              -- Unique identifier for the event record itself
                                           aggregate_id VARCHAR(255) NOT NULL,     -- ID of the aggregate (using VARCHAR to accommodate UUIDs or other formats if needed later)
    aggregate_type VARCHAR(255) NOT NULL,   -- Class name or logical type of the aggregate
    tenant_id UUID NOT NULL,                -- Tenant identifier
    sequence_number BIGINT NOT NULL,        -- Aggregate version after this event; MUST be > 0
    event_type VARCHAR(512) NOT NULL,       -- Class name or logical type of the DomainEvent
    event_payload VARBINARY NOT NULL,       -- Encrypted, serialized DomainEvent data
    event_version VARCHAR(50) NOT NULL,     -- Schema version of the DomainEvent (e.g., "V1")

-- Metadata columns
    encryption_algorithm_id VARCHAR(100) NOT NULL, -- Identifier of the encryption algorithm used
    key_context_id UUID NOT NULL,           -- Reference to the KeyContext used for encryption
    occurred_on TIMESTAMP WITH TIME ZONE NOT NULL, -- Timestamp when the event happened in the domain
    stored_on TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Timestamp when saved to DB
                            user_id UUID NOT NULL,                  -- User who caused the event

                            -- Constraint to ensure unique sequence number per aggregate instance per tenant
                            CONSTRAINT UQ_AGGREGATE_VERSION UNIQUE (aggregate_id, tenant_id, sequence_number)
    );

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS IDX_EVENT_STORE_AGG_ID_TENANT ON EVENT_STORE (aggregate_id, tenant_id);
CREATE INDEX IF NOT EXISTS IDX_EVENT_STORE_TENANT_TYPE ON EVENT_STORE (tenant_id, event_type);
CREATE INDEX IF NOT EXISTS IDX_EVENT_STORE_STORED_ON ON EVENT_STORE (stored_on);


-- AUDIT_LOG Table (Example structure)
CREATE TABLE IF NOT EXISTS AUDIT_LOG (
                                         log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                         tenant_id UUID NOT NULL,
                                         user_id UUID NULL, -- Can be null for system actions
                                         correlation_id UUID NULL,
                                         action VARCHAR(255) NOT NULL, -- e.g., KEY_SHARED, ACCESS_DENIED, GOAL_CREATED
    details CLOB NULL              -- CLOB for potentially large details (e.g., JSON)
    );

CREATE INDEX IF NOT EXISTS IDX_AUDIT_LOG_TENANT_USER ON AUDIT_LOG (tenant_id, user_id);
CREATE INDEX IF NOT EXISTS IDX_AUDIT_LOG_TIMESTAMP ON AUDIT_LOG (timestamp);
CREATE INDEX IF NOT EXISTS IDX_AUDIT_LOG_ACTION ON AUDIT_LOG (action);
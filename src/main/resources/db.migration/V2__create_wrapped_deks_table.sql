-- --- File: src/main/resources/db/migration/keys/V2__create_wrapped_deks_table.sql ---
-- Schema changes for managing individual Data Encryption Keys (DEKs)

-- WRAPPED_DEKS Table
CREATE TABLE IF NOT EXISTS WRAPPED_DEKS (
                                            dek_id UUID PRIMARY KEY,                -- The unique ID for this DEK (maps to DekId)
                                            owner_user_id UUID NOT NULL,            -- User who owns this specific DEK
                                            tenant_id UUID NOT NULL,                -- Tenant context
                                            wrapped_dek VARBINARY NOT NULL,         -- DEK wrapped by the owner's primary user key
                                            owner_key_id VARCHAR(255) NOT NULL,     -- Identifier of the owner's User Key used for wrapping
    algorithm_id VARCHAR(100) NOT NULL,     -- Algorithm used to wrap the DEK with the user key
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

                             -- Optional: Foreign key constraint to ensure owner user key exists
                             -- CONSTRAINT FK_DEK_OWNER_KEY FOREIGN KEY (owner_user_id, tenant_id) REFERENCES USER_KEYS(user_id, tenant_id) ON DELETE RESTRICT
                             -- Note: Add FK only if USER_KEYS are guaranteed to exist before DEKs are created for them. RESTRICT prevents deleting a user key if DEKs still reference it.

                             -- Constraint to ensure DekId uniqueness per tenant (dek_id is already PK, but adding tenant makes sense conceptually if needed)
                             -- CONSTRAINT UQ_DEK_TENANT UNIQUE (dek_id, tenant_id) -- dek_id is already PK, so this is redundant
                             );

-- Index for efficient lookup by owner
CREATE INDEX IF NOT EXISTS IDX_WRAPPED_DEKS_OWNER ON WRAPPED_DEKS (owner_user_id, tenant_id);

-- Update SHARE_GRANTS data_reference description (No actual SQL change, just comment)
-- COMMENT ON COLUMN SHARE_GRANTS.data_reference IS 'Identifier for the shared resource, MUST contain the UUID string representation of the DekId being shared.';

-- --- End File: src/main/resources/db/migration/keys/V2__create_wrapped_deks_table.sql ---
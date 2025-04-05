-- Schema for the dedicated security key and sharing grant database (Version 1)

-- USER_KEYS Table
CREATE TABLE IF NOT EXISTS USER_KEYS (
                                         user_id UUID NOT NULL,                  -- User identifier
                                         tenant_id UUID NOT NULL,                -- Tenant identifier
                                         wrapped_data_key VARBINARY NOT NULL,    -- User Data Key, encrypted with Tenant Master Key
                                         master_key_id VARCHAR(255) NOT NULL,    -- Identifier for the Tenant Master Key used for wrapping
    algorithm_id VARCHAR(100) NOT NULL,     -- Algorithm used to wrap the key (e.g., maybe specific to the master key type)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

                             -- Primary key covering user and tenant uniqueness
                             CONSTRAINT PK_USER_KEYS PRIMARY KEY (user_id, tenant_id)
    );

-- SHARE_GRANTS Table
CREATE TABLE IF NOT EXISTS SHARE_GRANTS (
                                            share_grant_id UUID PRIMARY KEY,        -- Unique identifier for the sharing grant
                                            tenant_id UUID NOT NULL,                -- Tenant where the sharing occurs
                                            owner_user_id UUID NOT NULL,            -- User who owns the original data/key
                                            data_reference VARCHAR(1024) NOT NULL,  -- Identifier for the shared data resource
    grantee_type VARCHAR(50) NOT NULL,      -- Type: USER, ROLE, TENANT
    grantee_id VARCHAR(255) NOT NULL,       -- Identifier of the grantee (UUID or String for Role)

-- Encrypted key data
    encrypted_data_key VARBINARY NOT NULL,  -- Owner's data key, re-encrypted using grantee's key/mechanism
    grantee_key_id VARCHAR(255) NOT NULL,   -- Identifier for the key/mechanism used to encrypt encrypted_data_key
    algorithm_id VARCHAR(100) NOT NULL,     -- Algorithm used to encrypt encrypted_data_key

-- Timestamps
    expiration_timestamp TIMESTAMP WITH TIME ZONE NULL, -- Null for permanent grants
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
                                    );

-- Indexes for common lookups
CREATE INDEX IF NOT EXISTS IDX_SHARE_GRANTS_TENANT_GRANTEE ON SHARE_GRANTS (tenant_id, grantee_type, grantee_id);
CREATE INDEX IF NOT EXISTS IDX_SHARE_GRANTS_DATA_REF ON SHARE_GRANTS (tenant_id, data_reference);
CREATE INDEX IF NOT EXISTS IDX_SHARE_GRANTS_EXPIRATION ON SHARE_GRANTS (expiration_timestamp);
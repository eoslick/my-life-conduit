package com.ses.mylifeconduit.core.security.encryption.domain;

import com.ses.mylifeconduit.core.ddd.domain.EntityId;

/**
 * Represents a subject (user, role, or tenant) that can be granted access to encrypted data.
 * This is a marker interface for different types of access subjects.
 */
public interface AccessSubject {
    
    /**
     * Gets the type of access subject (e.g., USER, ROLE, TENANT).
     * 
     * @return The subject type
     */
    AccessSubjectType getType();
    
    /**
     * Gets the unique identifier for this subject.
     * 
     * @return The subject's identifier
     */
    EntityId getId();
    
    /**
     * Enum defining the types of subjects that can access encrypted data.
     */
    enum AccessSubjectType {
        USER,
        ROLE,
        TENANT
    }
}
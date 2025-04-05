// src/main/java/com/ses/mylifeconduit/core/security/sharing/GranteeType.java
package com.ses.mylifeconduit.core.security.sharing;

/**
 * Enumerates the types of grantees that can be granted access to shared data.
 */
public enum GranteeType {
    /** A specific user. */
    USER,
    /** A defined role (group of users). */
    ROLE,
    /** An entire tenant (all users within that tenant). */
    TENANT
    // Potentially add GROUP later if needed
}
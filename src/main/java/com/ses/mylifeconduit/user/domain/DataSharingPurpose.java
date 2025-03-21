package com.ses.mylifeconduit.user.domain;

/**
 * Represents the business purpose for sharing data.
 * This helps users understand why they granted access and helps 
 * with auditing and compliance.
 */
public record DataSharingPurpose(String code, String description, String customReason) {
    
    /**
     * Creates a new data sharing purpose with a standard code and description.
     * 
     * @param code The unique code for this purpose
     * @param description The description of this purpose
     */
    public DataSharingPurpose(String code, String description) {
        this(code, description, null);
    }
    
    /**
     * Checks if this purpose has a custom reason.
     * 
     * @return True if a custom reason is provided, false otherwise
     */
    public boolean hasCustomReason() {
        return customReason != null && !customReason.isBlank();
    }
    
    /**
     * Gets the effective description, including the custom reason if available.
     * 
     * @return The effective description
     */
    public String getEffectiveDescription() {
        if (hasCustomReason()) {
            return description + ": " + customReason;
        }
        return description;
    }
    
    /**
     * Creates a purpose with a custom reason.
     * 
     * @param reason The custom reason for sharing
     * @return A new DataSharingPurpose with the OTHER code and the custom reason
     */
    public static DataSharingPurpose createCustom(String reason) {
        return new DataSharingPurpose("OTHER", "Custom purpose", reason);
    }
    
    /**
     * Pre-defined common sharing purposes.
     */
    public static class Common {
        public static final DataSharingPurpose COLLABORATION = 
            new DataSharingPurpose("COLLABORATION", "For collaboration on a shared project");
        
        public static final DataSharingPurpose DELEGATION = 
            new DataSharingPurpose("DELEGATION", "For delegated access to perform tasks");
        
        public static final DataSharingPurpose REVIEW = 
            new DataSharingPurpose("REVIEW", "For review and approval");
        
        public static final DataSharingPurpose SUPPORT = 
            new DataSharingPurpose("SUPPORT", "For customer support purposes");
        
        public static final DataSharingPurpose AUDIT = 
            new DataSharingPurpose("AUDIT", "For audit and compliance");
    }
}
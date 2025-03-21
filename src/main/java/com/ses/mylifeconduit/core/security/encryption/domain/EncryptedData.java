package com.ses.mylifeconduit.core.security.encryption.domain;

/**
 * Represents encrypted data with its metadata for decryption.
 * This is the format used for storing encrypted data at rest and in events.
 */
public class EncryptedData {
    private final byte[] ciphertext;
    private final EncryptionMetadataId encryptionMetadataId;
    private final EncryptionKeyId dataKeyId;
    
    /**
     * Creates a new encrypted data package.
     *
     * @param ciphertext The encrypted data
     * @param encryptionMetadataId The ID of the encryption algorithm used
     * @param dataKeyId The ID of the data key used for encryption
     */
    public EncryptedData(byte[] ciphertext, EncryptionMetadataId encryptionMetadataId, EncryptionKeyId dataKeyId) {
        this.ciphertext = ciphertext;
        this.encryptionMetadataId = encryptionMetadataId;
        this.dataKeyId = dataKeyId;
    }
    
    /**
     * Gets the encrypted data.
     *
     * @return The ciphertext
     */
    public byte[] getCiphertext() {
        return ciphertext;
    }
    
    /**
     * Gets the encryption algorithm metadata ID.
     *
     * @return The encryption metadata ID
     */
    public EncryptionMetadataId getEncryptionMetadataId() {
        return encryptionMetadataId;
    }
    
    /**
     * Gets the ID of the data key used for encryption.
     *
     * @return The data key ID
     */
    public EncryptionKeyId getDataKeyId() {
        return dataKeyId;
    }
}
package de.marcandreher.fusionkit.lib.helpers;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encryption utility class that uses AES-GCM encryption with the key from DataDirectory.
 * Provides secure encryption and decryption methods for sensitive data.
 */
public class EncryptionUtil {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits

    /**
     * Encrypts a plaintext string using AES-GCM encryption.
     * 
     * @param plaintext The string to encrypt
     * @return Base64-encoded encrypted data (IV + encrypted data), or null if encryption fails
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            logger.warn("Cannot encrypt null or empty string");
            return null;
        }

        try {
            // Get the encryption key from DataDirectory
            if (DataDirectory.encryptionKey == null) {
                logger.error("Encryption key not available. Make sure DataDirectory is initialized.");
                return null;
            }

            byte[] keyBytes = Base64.getDecoder().decode(DataDirectory.encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParams = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParams);

            // Encrypt the data
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);

        } catch (Exception e) {
            logger.error("Failed to encrypt data", e);
            return null;
        }
    }

    public static String encrypt(String... texts) {
        if (texts == null || texts.length == 0) {
            logger.warn("Cannot encrypt null or empty array");
            return null;
        }

        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < texts.length; i++) {
            if (texts[i] != null) {
                combined.append(texts[i]);
            }
            if (i < texts.length - 1) {
                combined.append(","); // Use comma as delimiter
            }
        }

        return encrypt(combined.toString());
    }

    /**
     * Decrypts a Base64-encoded encrypted string using AES-GCM.
     * 
     * @param encryptedData The Base64-encoded encrypted data (IV + encrypted data)
     * @return The decrypted plaintext string, or null if decryption fails
     */
    public static String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            logger.warn("Cannot decrypt null or empty string");
            return null;
        }

        try {
            // Get the encryption key from DataDirectory
            if (DataDirectory.encryptionKey == null) {
                logger.error("Encryption key not available. Make sure DataDirectory is initialized.");
                return null;
            }

            byte[] keyBytes = Base64.getDecoder().decode(DataDirectory.encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // Decode the encrypted data
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedData);

            if (encryptedWithIv.length < GCM_IV_LENGTH) {
                logger.error("Encrypted data is too short to contain IV");
                return null;
            }

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParams = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParams);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encrypted);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Failed to decrypt data", e);
            return null;
        }
    }

    /**
     * Encrypts sensitive data like passwords, API keys, etc.
     * 
     * @param sensitiveData The sensitive data to encrypt
     * @return Encrypted and Base64-encoded string, or null if encryption fails
     */
    public static String encryptSensitiveData(String sensitiveData) {
        if (sensitiveData == null) {
            return null;
        }
        
        logger.debug("Encrypting sensitive data");
        return encrypt(sensitiveData);
    }

    /**
     * Decrypts sensitive data like passwords, API keys, etc.
     * 
     * @param encryptedSensitiveData The encrypted sensitive data
     * @return Decrypted string, or null if decryption fails
     */
    public static String decryptSensitiveData(String encryptedSensitiveData) {
        if (encryptedSensitiveData == null) {
            return null;
        }
        
        logger.debug("Decrypting sensitive data");
        return decrypt(encryptedSensitiveData);
    }

    /**
     * Checks if the encryption system is properly initialized and working.
     * 
     * @return true if encryption is available and working, false otherwise
     */
    public static boolean isEncryptionAvailable() {
        if (DataDirectory.encryptionKey == null) {
            return false;
        }

        // Test encryption/decryption with a test string
        String testData = "test_encryption_" + System.currentTimeMillis();
        String encrypted = encrypt(testData);
        if (encrypted == null) {
            return false;
        }

        String decrypted = decrypt(encrypted);
        return testData.equals(decrypted);
    }

    /**
     * Encrypts a string and adds a prefix to identify it as encrypted.
     * Useful for database storage where you need to distinguish encrypted vs plain text.
     * 
     * @param plaintext The string to encrypt
     * @param prefix The prefix to add (e.g., "ENC:")
     * @return Prefixed encrypted string, or original string if encryption fails
     */
    public static String encryptWithPrefix(String plaintext, String prefix) {
        String encrypted = encrypt(plaintext);
        if (encrypted != null) {
            return prefix + encrypted;
        }
        logger.warn("Encryption failed, returning original string");
        return plaintext;
    }

    /**
     * Decrypts a string that has a prefix, or returns the original if not prefixed.
     * 
     * @param data The potentially encrypted string with prefix
     * @param prefix The prefix to check for (e.g., "ENC:")
     * @return Decrypted string if prefixed and decryptable, otherwise original string
     */
    public static String decryptIfPrefixed(String data, String prefix) {
        if (data == null || !data.startsWith(prefix)) {
            return data; // Not encrypted or null
        }

        String encryptedPart = data.substring(prefix.length());
        String decrypted = decrypt(encryptedPart);
        if (decrypted != null) {
            return decrypted;
        }
        
        logger.warn("Failed to decrypt prefixed data, returning original");
        return data;
    }
}
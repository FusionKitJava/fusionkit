package de.marcandreher.fusionkit.lib.helpers;

import java.io.FileNotFoundException;
import java.io.IOException;

public class DataDirectory {
    private static final String BASE_DIR = "data/";
    private static final String ENCRYPTION_KEY_FILE = "encryption.key";

    public static String encryptionKey;

    public DataDirectory() {
        // Ensure base data directory exists
        java.io.File baseDir = new java.io.File(BASE_DIR);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        generateKeyFile();

        loadEncryptionKey();

    }

    public void generateKeyFile() {
        java.io.File keyFile = new java.io.File(BASE_DIR + ENCRYPTION_KEY_FILE);
        if (!keyFile.exists()) {
            // Generate a new encryption key
            byte[] key = new byte[16]; // 128-bit key
            new java.security.SecureRandom().nextBytes(key);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(keyFile)) {
                fos.write(key);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadEncryptionKey()  {
        java.io.File keyFile = new java.io.File(BASE_DIR + ENCRYPTION_KEY_FILE);
        byte[] key = new byte[16];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(keyFile)) {
            if (fis.read(key) != 16) {
                throw new IOException("Invalid encryption key length");
            }
            encryptionKey = java.util.Base64.getEncoder().encodeToString(key);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

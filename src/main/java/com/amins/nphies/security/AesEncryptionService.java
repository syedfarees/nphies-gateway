package com.amins.nphies.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for NPHIES credentials.
 *
 * Format stored in DB: Base64( IV[12] + CipherText + AuthTag[16] )
 *
 * Why GCM over CBC:
 *  - Authenticated encryption — detects tampering (auth tag)
 *  - No padding oracle vulnerabilities
 *  - NIST recommended for symmetric encryption at rest
 */
@Service
@Slf4j
public class AesEncryptionService {

    private static final String ALGORITHM     = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LENGTH = 12;   // 96-bit IV — GCM standard
    private static final int    GCM_TAG_BITS  = 128;  // 128-bit authentication tag
    private static final int    KEY_SIZE_BITS = 256;

    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptionService(
            @Value("${nphies.encryption.master-key-base64}") String masterKeyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "NPHIES master key must be 256-bit (32 bytes). Got: " + keyBytes.length);
        }
        this.encryptionKey = new SecretKeySpec(keyBytes, "AES");
        log.info("AesEncryptionService initialized with AES-256-GCM");
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Each call generates a fresh random IV — safe for same key repeated use.
     *
     * @param plaintext raw secret (e.g. NPHIES client_secret)
     * @return Base64-encoded string: [12-byte IV][ciphertext][16-byte auth tag]
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, paramSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes());

            // Pack: IV || CipherText (includes GCM auth tag appended by Java)
            ByteBuffer packed = ByteBuffer.allocate(iv.length + cipherText.length);
            packed.put(iv);
            packed.put(cipherText);

            return Base64.getEncoder().encodeToString(packed.array());

        } catch (Exception e) {
            throw new NphiesEncryptionException("Failed to encrypt credential", e);
        }
    }

    /**
     * Decrypts a Base64-encoded AES-256-GCM ciphertext.
     * GCM auth tag verification is automatic — throws if tampered.
     *
     * @param encryptedBase64 value from DB
     * @return decrypted plaintext
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] packed = Base64.getDecoder().decode(encryptedBase64);

            // Unpack IV
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            // Remaining bytes = ciphertext + auth tag
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, paramSpec);

            byte[] plaintext = cipher.doFinal(cipherText);
            return new String(plaintext);

        } catch (Exception e) {
            throw new NphiesEncryptionException("Failed to decrypt credential — possible tampering", e);
        }
    }

    /**
     * Utility: generate a new random AES-256 key (Base64).
     * Run once at environment setup, store in AWS Parameter Store / Vault.
     */
    public static String generateNewMasterKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(KEY_SIZE_BITS, new SecureRandom());
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // ── inner exception ──────────────────────────────────────────────────────
    public static class NphiesEncryptionException extends RuntimeException {
        public NphiesEncryptionException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}

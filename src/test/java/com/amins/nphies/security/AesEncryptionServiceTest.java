package com.amins.nphies.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class AesEncryptionServiceTest {

    private AesEncryptionService service;

    // Fixed 256-bit test key (32 bytes, Base64-encoded)
    private static final String TEST_KEY_BASE64 =
            Base64.getEncoder().encodeToString(new byte[32]); // 32 zero bytes — test only

    @BeforeEach
    void setUp() {
        service = new AesEncryptionService(TEST_KEY_BASE64);
    }

    @Test
    void encryptDecrypt_roundTrip() {
        String plaintext = "my-super-secret-client-secret";
        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() {
        // Fresh random IV per call — same plaintext must never produce same ciphertext
        String plaintext = "same-secret";
        String enc1 = service.encrypt(plaintext);
        String enc2 = service.encrypt(plaintext);

        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void encrypt_outputIsBase64() {
        String encrypted = service.encrypt("test");
        // Should not throw
        assertThatCode(() -> Base64.getDecoder().decode(encrypted)).doesNotThrowAnyException();
    }

    @Test
    void decrypt_throwsOnTamperedCiphertext() {
        String encrypted = service.encrypt("original-secret");

        // Flip a byte in the middle of the Base64 to simulate tampering
        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length / 2] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(AesEncryptionService.NphiesEncryptionException.class)
                .hasMessageContaining("tamper");
    }

    @Test
    void decrypt_throwsOnGarbageInput() {
        assertThatThrownBy(() -> service.decrypt("not-valid-base64!!"))
                .isInstanceOf(AesEncryptionService.NphiesEncryptionException.class);
    }

    @Test
    void encryptDecrypt_worksWithSpecialCharacters() {
        String secret = "p@$$w0rd!#&*()=+[]{}|;':\",./<>?";
        assertThat(service.decrypt(service.encrypt(secret))).isEqualTo(secret);
    }

    @Test
    void encryptDecrypt_worksWithLongSecret() {
        String secret = "a".repeat(512);
        assertThat(service.decrypt(service.encrypt(secret))).isEqualTo(secret);
    }

    @Test
    void constructor_throwsOnWrongKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]); // 128-bit, not 256
        assertThatThrownBy(() -> new AesEncryptionService(shortKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("256-bit");
    }

    @Test
    void generateNewMasterKey_produces32ByteBase64Key() throws Exception {
        String key = AesEncryptionService.generateNewMasterKey();
        byte[] decoded = Base64.getDecoder().decode(key);
        assertThat(decoded).hasSize(32);
    }
}

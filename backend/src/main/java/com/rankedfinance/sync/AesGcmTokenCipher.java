package com.rankedfinance.sync;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM using only the JDK. GCM both encrypts and authenticates: a 16-byte tag is appended to
 * the ciphertext, and decrypting fails if any byte was changed or the key is wrong. Every value
 * gets a fresh random 12-byte nonce, so the same token never encrypts to the same bytes twice.
 *
 * <p>Stored layout: [key version: 1 byte][nonce: 12 bytes][ciphertext + tag]. The version byte is
 * passed to GCM as additional authenticated data (AAD): it isn't encrypted, but the tag covers it,
 * so changing it makes decrypting fail.
 */
@Component
class AesGcmTokenCipher implements TokenCipher {

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String NO_KEY = "ENCRYPTION_KEY is not set";

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key; // null when no key is configured
    private final int keyVersion;

    AesGcmTokenCipher(EncryptionProperties properties) {
        this.keyVersion = properties.keyVersion();
        this.key =
                properties.hasKey()
                        ? new SecretKeySpec(Base64.getDecoder().decode(properties.key()), "AES")
                        : null;
    }

    @Override
    public byte[] encrypt(String plaintext) {
        requireKey();
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(new byte[] {(byte) keyVersion});
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] stored = new byte[1 + NONCE_BYTES + encrypted.length];
            stored[0] = (byte) keyVersion;
            System.arraycopy(nonce, 0, stored, 1, NONCE_BYTES);
            System.arraycopy(encrypted, 0, stored, 1 + NONCE_BYTES, encrypted.length);
            return stored;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt the value", e);
        }
    }

    @Override
    public String decrypt(byte[] stored) {
        requireKey();
        if (stored.length < 1 + NONCE_BYTES + TAG_BITS / 8) {
            throw new IllegalStateException("Stored value is too short to decrypt");
        }
        if ((stored[0] & 0xFF) != keyVersion) {
            throw new IllegalStateException("Unknown key version " + (stored[0] & 0xFF));
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_BITS, stored, 1, NONCE_BYTES));
            cipher.updateAAD(new byte[] {stored[0]});
            byte[] plain =
                    cipher.doFinal(Arrays.copyOfRange(stored, 1 + NONCE_BYTES, stored.length));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // Deliberately generic: say nothing about the key or the value.
            throw new IllegalStateException("Could not decrypt the value", e);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new IllegalStateException(NO_KEY);
        }
    }
}

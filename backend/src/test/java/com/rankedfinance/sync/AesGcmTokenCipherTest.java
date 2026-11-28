package com.rankedfinance.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmTokenCipherTest {

    private static String randomKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static AesGcmTokenCipher cipher(String key) {
        return new AesGcmTokenCipher(new EncryptionProperties(key, 1));
    }

    @Test
    void roundTrips() {
        var cipher = cipher(randomKey());
        assertThat(cipher.decrypt(cipher.encrypt("access-sandbox-dummy")))
                .isEqualTo("access-sandbox-dummy");
    }

    @Test
    void sameTokenEncryptsDifferentlyEachTime() {
        var cipher = cipher(randomKey());
        assertThat(cipher.encrypt("token")).isNotEqualTo(cipher.encrypt("token"));
    }

    @Test
    void storesKeyVersionThenNonceThenCiphertext() {
        var cipher = new AesGcmTokenCipher(new EncryptionProperties(randomKey(), 7));
        byte[] stored = cipher.encrypt("abc");
        // 1 version byte + 12 nonce bytes + 3 plaintext bytes + 16 GCM tag bytes
        assertThat(stored).hasSize(1 + 12 + 3 + 16);
        assertThat(stored[0]).isEqualTo((byte) 7);
    }

    @Test
    void tamperedValueIsRejected() {
        var cipher = cipher(randomKey());
        byte[] stored = cipher.encrypt("token");
        stored[stored.length - 1] ^= 1;
        assertThatThrownBy(() -> cipher.decrypt(stored)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKeyIsRejected() {
        byte[] stored = cipher(randomKey()).encrypt("token");
        assertThatThrownBy(() -> cipher(randomKey()).decrypt(stored))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownKeyVersionIsRejected() {
        var cipher = cipher(randomKey());
        byte[] stored = cipher.encrypt("token");
        stored[0] = 2;
        assertThatThrownBy(() -> cipher.decrypt(stored))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key version");
    }

    @Test
    void refusesToWorkWithoutAKey() {
        var cipher = cipher("");
        assertThatThrownBy(() -> cipher.encrypt("token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ENCRYPTION_KEY is not set");
        assertThatThrownBy(() -> cipher.decrypt(new byte[40]))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ENCRYPTION_KEY is not set");
    }
}

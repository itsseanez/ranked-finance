package com.rankedfinance.sync;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * ENCRYPTION_KEY and ENCRYPTION_KEY_VERSION (ADR 0016).
 *
 * <p>{@code @ConfigurationProperties} fills this record from {@code app.encryption.*} in
 * application.yaml, much like reading {@code process.env} into a typed config object;
 * {@code @Validated} makes Spring check the constraints and refuse to start when one fails.
 *
 * <p>The key is blank (nothing encrypts yet) or base64 for exactly 32 bytes (AES-256). The check is
 * a boolean method rather than a pattern on the field so a failed start never prints the key.
 */
@ConfigurationProperties("app.encryption")
@Validated
public record EncryptionProperties(String key, @Min(1) @Max(255) int keyVersion) {

    @AssertTrue(message = "ENCRYPTION_KEY must be blank or base64 for exactly 32 bytes")
    boolean isKeyValid() {
        if (key == null || key.isBlank()) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(key).length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    boolean hasKey() {
        return key != null && !key.isBlank();
    }

    @Override
    public String toString() {
        return "EncryptionProperties[key="
                + (hasKey() ? "<set>" : "<unset>")
                + ", keyVersion="
                + keyVersion
                + "]";
    }
}

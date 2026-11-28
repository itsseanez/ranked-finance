package com.rankedfinance.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class EncryptionPropertiesTest {

    // ApplicationContextRunner starts a tiny Spring context with just the beans listed here, so the
    // test needs no database or web server.
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Configuration
    @EnableConfigurationProperties(EncryptionProperties.class)
    static class Config {}

    private static String keyOfLength(int bytes) {
        return Base64.getEncoder().encodeToString(new byte[bytes]);
    }

    @Test
    void shortKeyStopsStartup() {
        String key = keyOfLength(16);
        runner.withPropertyValues("app.encryption.key=" + key, "app.encryption.key-version=1")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            // The failure message must not echo the key.
                            Throwable cause = context.getStartupFailure();
                            while (cause != null) {
                                assertThat(String.valueOf(cause.getMessage())).doesNotContain(key);
                                cause = cause.getCause();
                            }
                        });
    }

    @Test
    void keyThatIsNotBase64StopsStartup() {
        runner.withPropertyValues("app.encryption.key=not base64!", "app.encryption.key-version=1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void keyVersionOutsideOneToTwoFiftyFiveStopsStartup() {
        runner.withPropertyValues("app.encryption.key=", "app.encryption.key-version=0")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("app.encryption.key=", "app.encryption.key-version=256")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankAndValidKeysStart() {
        runner.withPropertyValues("app.encryption.key=", "app.encryption.key-version=1")
                .run(context -> assertThat(context).hasNotFailed());
        runner.withPropertyValues(
                        "app.encryption.key=" + keyOfLength(32), "app.encryption.key-version=255")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void propertiesNeverPrintTheKey() {
        String key = keyOfLength(32);
        assertThat(new EncryptionProperties(key, 1).toString()).doesNotContain(key);
    }
}

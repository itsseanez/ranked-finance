package com.rankedfinance;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * A {@code @TestConfiguration} adds beans to the app context only in tests that {@code @Import} it.
 * This one swaps the real clock for a frozen one: 2026-11-28T04:30:00Z, which is still 2026-11-27
 * in Los Angeles. {@code @Primary} makes it win over ClockConfig's bean.
 */
@TestConfiguration
public class FixedClock {

    public static final Instant NOW = Instant.parse("2026-11-28T04:30:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}

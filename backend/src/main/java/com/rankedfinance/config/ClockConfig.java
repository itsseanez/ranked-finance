package com.rankedfinance.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Makes a {@link Clock} injectable. Code asks for "now" through it instead of calling now()
 * directly, so tests can swap in a frozen one (like passing a fake Date.now into a JS function). It
 * stays in UTC; callers apply the user's time zone with {@code clock.withZone(...)}.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

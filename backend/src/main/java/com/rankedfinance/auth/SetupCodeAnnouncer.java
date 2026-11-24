package com.rankedfinance.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Prints a generated setup code while setup is open (ADR 0012). */
@Component
class SetupCodeAnnouncer {

    private static final Logger log = LoggerFactory.getLogger(SetupCodeAnnouncer.class);

    private final SetupCode setupCode;
    private final Passkeys passkeys;

    SetupCodeAnnouncer(SetupCode setupCode, Passkeys passkeys) {
        this.setupCode = setupCode;
        this.passkeys = passkeys;
    }

    /** Runs once the app is ready, so the code is the last thing in the startup log. */
    @EventListener(ApplicationReadyEvent.class)
    void announce() {
        setupCode.startupMessage(passkeys.anyRegistered()).ifPresent(message -> log.info(message));
    }
}

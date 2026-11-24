package com.rankedfinance.auth;

import com.rankedfinance.user.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * First-run setup (ADR 0010, 0012, 0013). Until the first passkey exists, the setup code creates
 * the owner and opens a setup session that can register that passkey. Every rule about whether
 * setup is open lives here; callers ask this module instead of counting passkeys themselves.
 */
@Component
public class FirstRunSetup {

    private static final Logger log = LoggerFactory.getLogger(FirstRunSetup.class);
    private static final String STARS = "*".repeat(64);

    /** What happened when someone submitted a setup code. */
    sealed interface Outcome {}

    /** A passkey exists, so setup is closed for good. */
    record Closed() implements Outcome {}

    record WrongCode() implements Outcome {}

    /** The install's owner has a different email than OWNER_EMAIL. */
    record OwnerMismatch() implements Outcome {}

    /** The owner exists; start a setup session for this email. */
    record Opened(String email) implements Outcome {}

    private final SetupCode setupCode;
    private final Passkeys passkeys;
    private final UserRepository users;
    private final OwnerProperties owner;

    FirstRunSetup(
            SetupCode setupCode, Passkeys passkeys, UserRepository users, OwnerProperties owner) {
        this.setupCode = setupCode;
        this.passkeys = passkeys;
        this.users = users;
        this.owner = owner;
    }

    /** Setup is open until the first passkey exists. */
    public boolean isOpen() {
        return !passkeys.anyRegistered();
    }

    Outcome start(String submittedCode) {
        if (!isOpen()) {
            return new Closed();
        }
        if (!setupCode.matches(submittedCode)) {
            return new WrongCode();
        }
        return users.findOrCreateOwner(owner.email(), owner.displayName())
                .<Outcome>map(user -> new Opened(user.email()))
                .orElseGet(OwnerMismatch::new);
    }

    /**
     * Whether a passkey may be deleted: only while another remains, because deleting the last one
     * would reopen setup. Known limit: this is checked before Spring's delete runs, not in the same
     * transaction, so two simultaneous deletes could both pass. Passkey management moves to the
     * app's own endpoint in Milestone 7, where the check can share the delete's transaction.
     */
    boolean mayRemovePasskey() {
        return passkeys.count() > 1;
    }

    /**
     * Who may register a passkey: the owner, or a setup session while setup is open. A setup
     * session left over from before the first passkey existed gets 403.
     */
    public AuthorizationManager<RequestAuthorizationContext> passkeyRegistrationAccess() {
        return (authentication, context) -> {
            Set<String> authorities =
                    AuthorityUtils.authorityListToSet(authentication.get().getAuthorities());
            boolean isOwner = authorities.contains(Roles.authority(Roles.OWNER));
            boolean isOpenSetupSession =
                    authorities.contains(Roles.authority(Roles.SETUP)) && isOpen();
            return new AuthorizationDecision(isOwner || isOpenSetupSession);
        };
    }

    /**
     * What to print at startup (ADR 0012): the generated code while setup is open, and nothing
     * otherwise. A code the operator chose is never printed.
     */
    Optional<String> startupMessage() {
        if (!setupCode.isGenerated() || !isOpen()) {
            return Optional.empty();
        }
        return Optional.of(
                """

                %s
                Ranked Finance setup code:

                    %s

                Open the app, choose "First-time setup", and enter this code to create
                your passkey. It stops working once a passkey exists. While setup is
                open, the server prints a new code each time it starts.
                To choose your own code instead, set SETUP_CODE.
                %s"""
                        .formatted(STARS, setupCode.value(), STARS));
    }

    /** Runs once the app is ready, so the code is the last thing in the startup log. */
    @EventListener(ApplicationReadyEvent.class)
    void announce() {
        startupMessage().ifPresent(message -> log.info(message));
    }
}

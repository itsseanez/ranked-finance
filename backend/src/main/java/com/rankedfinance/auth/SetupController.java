package com.rankedfinance.auth;

import com.rankedfinance.user.User;
import com.rankedfinance.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * First-run setup (ADR 0010, 0013). Until a passkey exists, the right setup code creates the owner
 * and opens a setup-only session, so they can register their first passkey through Spring
 * Security's endpoints and then sign in with it.
 */
@RestController
@RequestMapping("/api/v1/setup")
class SetupController {

    record SetupRequest(String code) {
        // Spring MVC logs parsed request bodies at DEBUG; keep the code out of the logs.
        @Override
        public String toString() {
            return "SetupRequest[code=<redacted>]";
        }
    }

    private final SetupCode setupCode;
    private final OwnerProperties owner;
    private final Passkeys passkeys;
    private final UserRepository users;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolder =
            SecurityContextHolder.getContextHolderStrategy();

    SetupController(
            SetupCode setupCode, OwnerProperties owner, Passkeys passkeys, UserRepository users) {
        this.setupCode = setupCode;
        this.owner = owner;
        this.passkeys = passkeys;
        this.users = users;
    }

    @PostMapping
    ResponseEntity<ProblemDetail> setup(
            @RequestBody SetupRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (passkeys.anyRegistered()) {
            return error(HttpStatus.FORBIDDEN, "setup_closed", "This app is already set up.");
        }
        if (!setupCode.matches(body.code())) {
            return error(
                    HttpStatus.UNAUTHORIZED, "invalid_setup_code", "That setup code isn't right.");
        }

        Optional<User> ownerRow =
                users.count() == 0
                        ? Optional.of(users.createOwner(owner.email(), owner.displayName()))
                        : users.findByEmail(owner.email());
        if (ownerRow.isEmpty()) {
            return error(
                    HttpStatus.CONFLICT,
                    "owner_mismatch",
                    "OWNER_EMAIL doesn't match the owner already on this server.");
        }
        startSetupSession(ownerRow.get().email(), request, response);
        return ResponseEntity.noContent().build();
    }

    /**
     * A session that can only register a passkey (ADR 0013). Using the app takes a real passkey
     * sign-in afterwards, so a browser whose passkey setup failed isn't left signed in without one.
     * The principal's name is the owner's email, which names the passkey's WebAuthn user entity.
     */
    private void startSetupSession(
            String email, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_SETUP")));
        // A new session ID and CSRF token, so ones planted before setup are useless afterwards.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        CookieCsrfTokenRepository.withHttpOnlyFalse().saveToken(null, request, response);
        SecurityContext context = securityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private static ResponseEntity<ProblemDetail> error(
            HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("error", code);
        return ResponseEntity.status(status).body(problem);
    }
}

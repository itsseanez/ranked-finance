package com.rankedfinance.auth;

import com.rankedfinance.user.User;
import com.rankedfinance.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * First-run setup (ADR 0010). Until a passkey exists, the right setup code creates the owner and
 * signs them in, so they can register their first passkey through Spring Security's endpoints.
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
    private final OwnerUserDetailsService userDetails;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolder =
            SecurityContextHolder.getContextHolderStrategy();

    SetupController(
            SetupCode setupCode,
            OwnerProperties owner,
            Passkeys passkeys,
            UserRepository users,
            OwnerUserDetailsService userDetails) {
        this.setupCode = setupCode;
        this.owner = owner;
        this.passkeys = passkeys;
        this.users = users;
        this.userDetails = userDetails;
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

        User ownerRow =
                users.count() == 0
                        ? users.createOwner(owner.email(), owner.displayName())
                        : users.findByEmail(owner.email())
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "the existing owner's email doesn't match"
                                                                + " OWNER_EMAIL"));
        signIn(ownerRow.email(), request, response);
        return ResponseEntity.noContent().build();
    }

    private void signIn(String email, HttpServletRequest request, HttpServletResponse response) {
        UserDetails principal = userDetails.loadUserByUsername(email);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities());
        // A new session ID on sign-in, so a session ID planted before sign-in is useless
        // afterwards.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
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

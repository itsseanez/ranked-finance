package com.rankedfinance.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/**
 * Starts a setup session (ADR 0013): it can register the first passkey and nothing else. Uses the
 * same CSRF and security-context repositories as the security chain (beans in SecurityConfig), so
 * the two can't drift apart.
 */
@Component
class SetupSession {

    private final CsrfTokenRepository csrfTokens;
    private final SecurityContextRepository securityContexts;
    private final SecurityContextHolderStrategy securityContextHolder =
            SecurityContextHolder.getContextHolderStrategy();

    SetupSession(CsrfTokenRepository csrfTokens, SecurityContextRepository securityContexts) {
        this.csrfTokens = csrfTokens;
        this.securityContexts = securityContexts;
    }

    /**
     * The principal's name is the owner's email, which names the passkey's WebAuthn user entity.
     */
    void start(String email, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority(Roles.authority(Roles.SETUP))));
        // A new session ID and CSRF token, so ones planted before setup are useless afterwards.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        csrfTokens.saveToken(null, request, response);
        SecurityContext context = securityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);
    }
}

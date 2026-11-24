package com.rankedfinance.auth;

import com.rankedfinance.common.ApiProblems;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Refuses to delete the only passkey. Setup stays closed while a passkey exists (ADR 0010), so
 * deleting the last one would let anyone with the setup code claim the instance again.
 */
@Component
public class LastPasskeyGuard {

    private static final RequestMatcher DELETE_PASSKEY =
            PathPatternRequestMatcher.withDefaults()
                    .matcher(HttpMethod.DELETE, "/webauthn/register/{id}");

    private final Passkeys passkeys;
    private final JsonMapper json;

    LastPasskeyGuard(Passkeys passkeys, JsonMapper json) {
        this.passkeys = passkeys;
        this.json = json;
    }

    /**
     * The filter, for SecurityConfig to add after AuthorizationFilter, where Spring also puts
     * WebAuthnRegistrationFilter (which handles {@code DELETE /webauthn/register/{id}}). Added
     * first, it runs just before that filter. It isn't a bean itself, so Spring Boot doesn't also
     * register it as a plain servlet filter that runs before sign-in.
     */
    public Filter filter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                if (DELETE_PASSKEY.matches(request) && passkeys.count() <= 1) {
                    ApiProblems.write(
                            response,
                            HttpStatus.CONFLICT,
                            "last_passkey",
                            "You can't delete your only passkey. Add another one first.",
                            json);
                    return;
                }
                chain.doFilter(request, response);
            }
        };
    }
}

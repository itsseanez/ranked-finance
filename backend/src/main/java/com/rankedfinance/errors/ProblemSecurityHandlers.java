package com.rankedfinance.errors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Security's 401 and 403 answers, as problem details. These run in the security filter
 * chain, before Spring MVC, so they write the response directly (ApiProblems.write).
 */
@Component
public class ProblemSecurityHandlers {

    private final JsonMapper json;

    ProblemSecurityHandlers(JsonMapper json) {
        this.json = json;
    }

    /** Not signed in. */
    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) ->
                ApiProblems.write(
                        response, ErrorCode.UNAUTHENTICATED, "Sign in to continue.", json);
    }

    /**
     * A refused sign-in (a passkey the relying party rejected). The detail is fixed: why it failed
     * stays on the server.
     */
    public AuthenticationFailureHandler signInFailureHandler() {
        return (request, response, ex) ->
                ApiProblems.write(
                        response,
                        ErrorCode.UNAUTHENTICATED,
                        "That passkey couldn't sign you in.",
                        json);
    }

    /**
     * Signed in but refused: {@code setup_incomplete} for a setup session (it can only register the
     * first passkey), otherwise {@code forbidden}. A CSRF failure is always {@code forbidden}: it
     * means a forged or stale request, whoever sent it.
     *
     * @param setupAuthority the authority a setup session has (passed in, so this module doesn't
     *     depend on auth)
     */
    public AccessDeniedHandler accessDeniedHandler(String setupAuthority) {
        return (request, response, ex) -> {
            Authentication authentication =
                    SecurityContextHolder.getContextHolderStrategy()
                            .getContext()
                            .getAuthentication();
            boolean setupSession =
                    authentication != null
                            && authentication.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .anyMatch(setupAuthority::equals);
            if (setupSession && !(ex instanceof CsrfException)) {
                ApiProblems.write(
                        response,
                        ErrorCode.SETUP_INCOMPLETE,
                        "Finish setting up your passkey first.",
                        json);
            } else {
                ApiProblems.write(response, ErrorCode.FORBIDDEN, "You can't do that.", json);
            }
        };
    }
}

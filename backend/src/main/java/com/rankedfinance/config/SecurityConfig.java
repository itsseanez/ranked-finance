package com.rankedfinance.config;

import com.rankedfinance.auth.FirstRunSetup;
import com.rankedfinance.auth.LastPasskeyGuard;
import com.rankedfinance.auth.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;

/**
 * Passkey-first sign-in (ADR 0010): no passwords, no form login, a 401 for anything
 * unauthenticated.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            WebAuthnProperties webAuthn,
            FirstRunSetup firstRunSetup,
            LastPasskeyGuard lastPasskeyGuard,
            CsrfTokenRepository csrfTokens,
            SecurityContextRepository securityContexts)
            throws Exception {
        http.authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(HttpMethod.POST, "/api/v1/setup")
                                        .permitAll()
                                        .requestMatchers("/actuator/health")
                                        .permitAll()
                                        // Errors are forwarded to /error; without this, every
                                        // anonymous error (a CSRF 403, a 400) became an empty 401.
                                        .requestMatchers("/error")
                                        .permitAll()
                                        // Only exists in dev; see springdoc.api-docs.enabled.
                                        .requestMatchers("/v3/api-docs/**")
                                        .permitAll()
                                        // The owner, or a setup session while setup is open.
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/webauthn/register/options",
                                                "/webauthn/register")
                                        .access(firstRunSetup.passkeyRegistrationAccess())
                                        .requestMatchers("/logout")
                                        .authenticated()
                                        .anyRequest()
                                        .hasRole(Roles.OWNER))
                .webAuthn(
                        passkeys ->
                                passkeys.rpName("Ranked Finance")
                                        .rpId(webAuthn.rpId())
                                        .allowedOrigins(webAuthn.origins())
                                        // Its delete button would bypass LastPasskeyGuard's rule.
                                        .disableDefaultRegistrationPage(true))
                // Same spot Spring puts WebAuthnRegistrationFilter; added first, so it runs just
                // before it.
                .addFilterAfter(lastPasskeyGuard.filter(), AuthorizationFilter.class)
                // The SPA reads the XSRF-TOKEN cookie and sends it back in the X-XSRF-TOKEN header.
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokens))
                .securityContext(context -> context.securityContextRepository(securityContexts))
                .exceptionHandling(
                        exceptions ->
                                exceptions.authenticationEntryPoint(
                                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(
                        logout ->
                                logout.logoutSuccessHandler(
                                        new HttpStatusReturningLogoutSuccessHandler(
                                                HttpStatus.NO_CONTENT)));
        return http.build();
    }

    /**
     * The CSRF repository csrf.spa() would create, as a bean so SetupSession clears the same cookie
     * the chain checks.
     */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /** Spring Security's default security-context repository, shared with SetupSession. */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean
    JdbcPublicKeyCredentialUserEntityRepository passkeyUserEntities(JdbcOperations jdbc) {
        return new JdbcPublicKeyCredentialUserEntityRepository(jdbc);
    }

    @Bean
    JdbcUserCredentialRepository passkeyCredentials(JdbcOperations jdbc) {
        return new JdbcUserCredentialRepository(jdbc);
    }
}

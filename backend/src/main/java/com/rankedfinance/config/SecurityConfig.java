package com.rankedfinance.config;

import com.rankedfinance.auth.FirstRunSetup;
import com.rankedfinance.auth.LastPasskeyGuard;
import com.rankedfinance.auth.OwnerSignIn;
import com.rankedfinance.auth.Roles;
import com.rankedfinance.errors.ProblemSecurityHandlers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationProvider;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

/**
 * Passkey-first sign-in (ADR 0010): no passwords, no form login, a 401 for anything
 * unauthenticated.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FirstRunSetup firstRunSetup,
            LastPasskeyGuard lastPasskeyGuard,
            CsrfTokenRepository csrfTokens,
            SecurityContextRepository securityContexts,
            WebAuthnRelyingPartyOperations passkeyOperations,
            UserDetailsService userDetailsService,
            OwnerSignIn ownerSignIn,
            ObjectProvider<AuthenticationEventPublisher> eventPublisher,
            ProblemSecurityHandlers problems)
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
                // The relying party (name, ID, origins) is the passkeyOperations bean below.
                .webAuthn(
                        passkeys ->
                                passkeys.withObjectPostProcessor(
                                                ownerPasskeySignIn(
                                                        passkeyOperations,
                                                        userDetailsService,
                                                        ownerSignIn,
                                                        eventPublisher,
                                                        problems))
                                        // Its delete button would bypass LastPasskeyGuard's rule.
                                        .disableDefaultRegistrationPage(true))
                // Same spot Spring puts WebAuthnRegistrationFilter; added first, so it runs just
                // before it.
                .addFilterAfter(lastPasskeyGuard.filter(), AuthorizationFilter.class)
                // The SPA reads the XSRF-TOKEN cookie and sends it back in the X-XSRF-TOKEN header.
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokens))
                .securityContext(context -> context.securityContextRepository(securityContexts))
                // Every 401 and 403 is a problem detail with a code; the CSRF filter uses the same
                // access-denied handler.
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(problems.entryPoint())
                                        .accessDeniedHandler(
                                                problems.accessDeniedHandler(
                                                        Roles.authority(Roles.SETUP))))
                .logout(
                        logout ->
                                logout.logoutSuccessHandler(
                                        new HttpStatusReturningLogoutSuccessHandler(
                                                HttpStatus.NO_CONTENT)));
        return http.build();
    }

    /**
     * Makes a passkey sign-in produce the owner's id (OwnerSignIn) instead of Spring's WebAuthn
     * user entity. Spring hands objects a configurer builds to its ObjectPostProcessors so apps can
     * adjust them (AbstractHttpConfigurer.withObjectPostProcessor); WebAuthnConfigurer.configure()
     * passes its WebAuthnAuthenticationFilter through postProcess. Its existing authentication
     * manager isn't readable, so this builds the same one Spring does (a ProviderManager around
     * WebAuthnAuthenticationProvider, from the same passkeyOperations bean) and maps its result.
     */
    private static ObjectPostProcessor<WebAuthnAuthenticationFilter> ownerPasskeySignIn(
            WebAuthnRelyingPartyOperations passkeyOperations,
            UserDetailsService userDetailsService,
            OwnerSignIn ownerSignIn,
            ObjectProvider<AuthenticationEventPublisher> eventPublisher,
            ProblemSecurityHandlers problems) {
        // The explicit type argument is what Spring reads to call this only for that filter.
        return new ObjectPostProcessor<WebAuthnAuthenticationFilter>() {
            @Override
            public <O extends WebAuthnAuthenticationFilter> O postProcess(O filter) {
                ProviderManager passkeyCheck =
                        new ProviderManager(
                                new WebAuthnAuthenticationProvider(
                                        passkeyOperations, userDetailsService));
                eventPublisher.ifAvailable(passkeyCheck::setAuthenticationEventPublisher);
                filter.setAuthenticationManager(
                        request ->
                                ownerSignIn.signedIn(passkeyCheck.authenticate(request).getName()));
                // A refused passkey answers with a 401 problem detail, like every other 401.
                filter.setAuthenticationFailureHandler(problems.signInFailureHandler());
                return filter;
            }
        };
    }

    /**
     * The relying party: who passkeys belong to and which origins may use them. As a bean,
     * WebAuthnConfigurer uses it for registration and sign-in, and ownerPasskeySignIn reuses it.
     */
    @Bean
    WebAuthnRelyingPartyOperations passkeyOperations(
            JdbcPublicKeyCredentialUserEntityRepository userEntities,
            JdbcUserCredentialRepository credentials,
            WebAuthnProperties webAuthn) {
        return new Webauthn4JRelyingPartyOperations(
                userEntities,
                credentials,
                PublicKeyCredentialRpEntity.builder()
                        .id(webAuthn.rpId())
                        .name("Ranked Finance")
                        .build(),
                webAuthn.origins());
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

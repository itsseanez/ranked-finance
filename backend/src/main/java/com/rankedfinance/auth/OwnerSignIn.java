package com.rankedfinance.auth;

import com.rankedfinance.user.OwnerPrincipal;
import com.rankedfinance.user.UserRepository;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Turns a successful passkey sign-in, which names the owner by email, into the signed-in owner with
 * their id (see SecurityConfig).
 */
@Component
public class OwnerSignIn {

    private final UserRepository users;

    OwnerSignIn(UserRepository users) {
        this.users = users;
    }

    public Authentication signedIn(String email) {
        return users.findByEmail(email)
                .map(
                        user ->
                                UsernamePasswordAuthenticationToken.authenticated(
                                        new OwnerPrincipal(user.id(), email),
                                        null,
                                        List.of(
                                                new SimpleGrantedAuthority(
                                                        Roles.authority(Roles.OWNER)))))
                .orElseThrow(() -> new BadCredentialsException("no user with that email"));
    }
}

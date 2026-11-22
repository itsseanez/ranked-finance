package com.rankedfinance.auth;

import com.rankedfinance.user.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads the signed-in user by email, which is the passkey's user-entity name. There are no
 * passwords (ADR 0010); the empty password only satisfies Spring's UserDetails type.
 */
@Service
public class OwnerUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public OwnerUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(email)
                .map(
                        user ->
                                User.withUsername(user.email())
                                        .password("")
                                        .authorities("ROLE_OWNER")
                                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("no user with that email"));
    }
}

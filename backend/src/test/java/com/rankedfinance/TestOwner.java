package com.rankedfinance;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.rankedfinance.auth.Roles;
import com.rankedfinance.user.OwnerPrincipal;
import com.rankedfinance.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The install's owner for tests. A real passkey sign-in can't run in MockMvc, so requests sign in
 * with the same principal a passkey sign-in produces (see OwnerSignIn).
 */
public final class TestOwner {

    public static final String EMAIL = "owner@example.com";

    private TestOwner() {}

    /** Creates the owner row and returns its id. */
    public static UUID create(UserRepository users) {
        return users.createOwner(EMAIL, "Owner").id();
    }

    /** Makes a MockMvc request as the signed-in owner. */
    public static RequestPostProcessor asOwner(UUID id, String email) {
        return authentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new OwnerPrincipal(id, email),
                        null,
                        List.of(new SimpleGrantedAuthority(Roles.authority(Roles.OWNER)))));
    }
}

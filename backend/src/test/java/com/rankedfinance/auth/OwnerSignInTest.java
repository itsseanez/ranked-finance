package com.rankedfinance.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.user.OwnerPrincipal;
import com.rankedfinance.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

// A passkey sign-in names the owner by email; controllers need their id.
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserRepository.class, OwnerSignIn.class})
class OwnerSignInTest {

    @Autowired UserRepository users;
    @Autowired OwnerSignIn ownerSignIn;

    @Test
    void mapsTheOwnersEmailToTheirId() {
        UUID id = users.createOwner("owner@example.com", "Owner").id();

        Authentication authentication = ownerSignIn.signedIn("owner@example.com");

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new OwnerPrincipal(id, "owner@example.com"));
        // Spring's passkey registration finds the user entity by this name.
        assertThat(authentication.getName()).isEqualTo("owner@example.com");
        assertThat(AuthorityUtils.authorityListToSet(authentication.getAuthorities()))
                .containsExactly(Roles.authority(Roles.OWNER));
    }

    @Test
    void unknownEmailIsRefused() {
        users.createOwner("owner@example.com", "Owner");

        assertThatThrownBy(() -> ownerSignIn.signedIn("stranger@example.com"))
                .isInstanceOf(BadCredentialsException.class);
    }
}

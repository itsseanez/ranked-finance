package com.rankedfinance.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.rankedfinance.TestOwner;
import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

// Spring's passkey login asks this service for the user's roles before OwnerSignIn replaces it.
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserRepository.class, OwnerUserDetailsService.class})
class OwnerUserDetailsServiceTest {

    @Autowired UserRepository users;
    @Autowired OwnerUserDetailsService userDetailsService;

    @Test
    void ownerGetsTheOwnerRole() {
        TestOwner.create(users);

        UserDetails owner = userDetailsService.loadUserByUsername(TestOwner.EMAIL);

        assertThat(owner.getUsername()).isEqualTo(TestOwner.EMAIL);
        assertThat(AuthorityUtils.authorityListToSet(owner.getAuthorities()))
                .containsExactly(Roles.authority(Roles.OWNER));
    }
}

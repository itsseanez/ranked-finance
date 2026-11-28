package com.rankedfinance.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestDatabase;
import com.rankedfinance.TestOwner;
import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.auth.Roles;
import com.rankedfinance.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired UserRepository users;

    @AfterEach
    void deleteEverything() {
        TestDatabase.deleteAllRows(jdbcClient);
    }

    @Test
    void apiRequiresAuth() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error").value("unauthenticated"));
    }

    // A setup session can register the first passkey and nothing else (ADR 0013); the code tells
    // the frontend to finish setup rather than show a generic refusal.
    @Test
    void setupSessionGetsSetupIncomplete() throws Exception {
        TestOwner.create(users);

        mvc.perform(get("/api/v1/me").with(user(TestOwner.EMAIL).roles(Roles.SETUP)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("setup_incomplete"));
    }

    // A missing CSRF token is a forged request, never "finish setup".
    @Test
    void setupSessionWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/logout").with(user(TestOwner.EMAIL).roles(Roles.SETUP)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    // A stranger must not be able to start registering a passkey without a session. Spring's
    // registration filter refuses this itself (with 400) before our 401 entry point runs, so the
    // test pins what matters: the request is refused and no registration was started.
    @Test
    void anonymousCannotRequestRegistrationOptions() throws Exception {
        mvc.perform(post("/webauthn/register/options").with(csrf()))
                .andExpect(status().isBadRequest());

        long userEntities =
                jdbcClient.sql("select count(*) from user_entities").query(Long.class).single();
        assertThat(userEntities).isZero();
    }

    // A forged request from another site carries the session cookie but not the CSRF token.
    @Test
    @WithMockUser
    void postWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
    }

    // Spring's registration names the passkey's user entity after Authentication.getName(), so the
    // owner's principal must keep the email as its name or a second user entity would appear.
    @Test
    void ownerCanStillRequestPasskeyOptions() throws Exception {
        UUID id = TestOwner.create(users);

        mvc.perform(
                        post("/webauthn/register/options")
                                .with(TestOwner.asOwner(id, TestOwner.EMAIL))
                                .with(csrf()))
                .andExpect(status().isOk());

        List<String> userEntityNames =
                jdbcClient.sql("select name from user_entities").query(String.class).list();
        assertThat(userEntityNames).containsExactly(TestOwner.EMAIL);
    }
}

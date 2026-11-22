package com.rankedfinance.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbcClient;

    @Test
    void apiRequiresAuth() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    // A stranger must not be able to start registering a passkey without a session. Spring's
    // registration filter refuses this itself (with 400) before our 401 entry point runs, so the
    // test pins what matters: the request is refused and no registration was started.
    @Test
    void anonymousCannotRequestRegistrationOptions() throws Exception {
        mvc.perform(post("/webauthn/register/options").with(csrf()))
                .andExpect(status().is4xxClientError());

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
}

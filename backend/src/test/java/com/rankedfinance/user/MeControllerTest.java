package com.rankedfinance.user;

import static com.rankedfinance.TestOwner.asOwner;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestDatabase;
import com.rankedfinance.TestOwner;
import com.rankedfinance.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

// Checks /api/v1/me end to end, signing in through first-run setup since there's no login screen
// yet.
@SpringBootTest(properties = "app.setup.code=test-setup-code-0123456789")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired UserRepository users;

    @AfterEach
    void deleteEverything() {
        TestDatabase.deleteAllRows(jdbcClient);
    }

    // Setup creates the owner row. A setup session can't use the app (ADR 0013), and a real passkey
    // sign-in can't run in MockMvc, so the request is made as the signed-in owner.
    @Test
    void returnsTheSignedInOwner() throws Exception {
        mvc.perform(
                        post("/api/v1/setup")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"test-setup-code-0123456789\"}"))
                .andExpect(status().isNoContent());

        User owner = users.findByEmail(TestOwner.EMAIL).orElseThrow();

        mvc.perform(get("/api/v1/me").with(asOwner(owner.id(), owner.email())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.id().toString()))
                .andExpect(jsonPath("$.displayName").value("Owner"))
                .andExpect(jsonPath("$.email").value("owner@example.com"));
    }
}

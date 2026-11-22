package com.rankedfinance.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

// Checks /api/v1/me end to end, signing in through first-run setup since there's no login screen
// yet.
@SpringBootTest(properties = "app.setup.code=test-setup-code-0123456789")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbcClient;

    @AfterEach
    void deleteEverything() {
        jdbcClient.sql("delete from users").update();
    }

    @Test
    void returnsTheSignedInOwner() throws Exception {
        MockHttpSession session =
                (MockHttpSession)
                        mvc.perform(
                                        post("/api/v1/setup")
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        "{\"code\":\"test-setup-code-0123456789\"}"))
                                .andExpect(status().isNoContent())
                                .andReturn()
                                .getRequest()
                                .getSession(false);

        mvc.perform(get("/api/v1/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.displayName").value("Owner"))
                .andExpect(jsonPath("$.email").value("owner@example.com"));
    }
}

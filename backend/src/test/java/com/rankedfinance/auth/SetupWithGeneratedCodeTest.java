package com.rankedfinance.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestDatabase;
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
import org.springframework.test.web.servlet.ResultActions;

// With no SETUP_CODE, the server generates a setup code at startup (ADR 0012). That code opens
// setup. (An empty code never matching is pinned in SetupCodeTest.)
@SpringBootTest(properties = "app.setup.code=")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SetupWithGeneratedCodeTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired SetupCode setupCode;

    // MockMvc requests commit, so each test cleans up after itself.
    @AfterEach
    void deleteEverything() {
        TestDatabase.deleteAllRows(jdbcClient);
    }

    @Test
    void generatedCodeSignsInTheOwner() throws Exception {
        setup(setupCode.value()).andExpect(status().isNoContent());
    }

    private ResultActions setup(String code) throws Exception {
        return mvc.perform(
                post("/api/v1/setup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"));
    }
}

package com.rankedfinance.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// With no SETUP_CODE configured, setup is unavailable: an empty code must never match an empty
// setting.
@SpringBootTest(properties = "app.setup.code=")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SetupNotConfiguredTest {

    @Autowired MockMvc mvc;

    @Test
    void emptyCodeNeverMatches() throws Exception {
        mvc.perform(
                        post("/api/v1/setup")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("setup_not_configured"));
    }
}

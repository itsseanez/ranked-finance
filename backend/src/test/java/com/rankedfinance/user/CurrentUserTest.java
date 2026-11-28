package com.rankedfinance.user;

import static com.rankedfinance.TestOwner.asOwner;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestDatabase;
import com.rankedfinance.TestOwner;
import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.auth.Roles;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

// Controllers take a CurrentUser parameter; every query they make is scoped to its id.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CurrentUserTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JdbcClient jdbcClient;

    @AfterEach
    void deleteEverything() {
        TestDatabase.deleteAllRows(jdbcClient);
    }

    @Test
    void controllerReceivesTheOwnersId() throws Exception {
        UUID id = TestOwner.create(users);

        mvc.perform(get("/api/v1/me").with(asOwner(id, TestOwner.EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // A setup session can only register the first passkey (ADR 0013).
    @Test
    void setupSessionNeverReachesAController() throws Exception {
        TestOwner.create(users);

        mvc.perform(get("/api/v1/me").with(user(TestOwner.EMAIL).roles(Roles.SETUP)))
                .andExpect(status().isForbidden());
    }
}

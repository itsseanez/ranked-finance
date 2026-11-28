package com.rankedfinance.accounts;

import static com.rankedfinance.TestOwner.asOwner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.FixedClock;
import com.rankedfinance.TestDatabase;
import com.rankedfinance.TestOwner;
import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.user.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// The clock is frozen at 2026-11-28T04:30Z and the owner lives in Los Angeles, so "today" is
// 2026-11-27 for everything here.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClock.class})
class AccountControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;

    UUID ownerId;
    RequestPostProcessor owner;

    @BeforeEach
    void createOwner() {
        ownerId = TestOwner.create(users);
        jdbcClient
                .sql("update users set time_zone = 'America/Los_Angeles' where id = :id")
                .param("id", ownerId)
                .update();
        owner = asOwner(ownerId, TestOwner.EMAIL);
    }

    @AfterEach
    void deleteEverything() {
        TestDatabase.deleteAllRows(jdbcClient);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String json)
            throws Exception {
        return mvc.perform(
                request.with(owner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private static String createJson(String name, String kind, String balance, String apr) {
        return """
                {"name":"%s","kind":"%s","balance":"%s"%s}
                """
                .formatted(name, kind, balance, apr == null ? "" : ",\"apr\":\"" + apr + "\"");
    }

    private UUID create(String name, String kind, String balance) throws Exception {
        String body =
                send(post("/api/v1/accounts/manual"), createJson(name, kind, balance, null))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.<String>read(body, "$.id"));
    }

    private int snapshotCount(UUID id) {
        return jdbcClient
                .sql("select count(*) from balance_snapshots where account_id = :id")
                .param("id", id)
                .query(Integer.class)
                .single();
    }

    @Test
    void balanceIsDatedInTheUsersTimeZone() throws Exception {
        send(post("/api/v1/accounts/manual"), createJson("Savings", "cash", "10", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAsOf").value("2026-11-27"));
    }

    @Test
    void secondBalanceTheSameDayReplacesTheFirst() throws Exception {
        UUID id = create("Savings", "cash", "10");
        send(put("/api/v1/accounts/" + id + "/balance"), "{\"balance\":\"20\"}")
                .andExpect(status().isOk());
        send(put("/api/v1/accounts/" + id + "/balance"), "{\"balance\":\"30.5\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("30.50"));
        assertThat(snapshotCount(id)).isEqualTo(1);
    }

    @Test
    void moneyIsSentAsDecimalStrings() throws Exception {
        send(post("/api/v1/accounts/manual"), createJson("Brokerage", "investment", "8950", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value("8950.00"))
                .andExpect(jsonPath("$.balance").isString())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.updateReminder").value("monthly"));
    }

    @Test
    void manualDebtRequiresAnApr() throws Exception {
        send(post("/api/v1/accounts/manual"), createJson("Card", "debt", "500", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("apr"));
    }

    @Test
    void aprOutsideZeroToOneHundredIsRejected() throws Exception {
        send(post("/api/v1/accounts/manual"), createJson("Card", "debt", "500", "100.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("apr"));
    }

    @Test
    void negativeBalanceIsOnlyAllowedForCash() throws Exception {
        send(post("/api/v1/accounts/manual"), createJson("Fund", "investment", "-5.00", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("balance"));
        send(post("/api/v1/accounts/manual"), createJson("Checking", "cash", "-5.00", null))
                .andExpect(status().isCreated());
    }

    @Test
    void fractionsOfACentAreRejected() throws Exception {
        send(post("/api/v1/accounts/manual"), createJson("Checking", "cash", "8.999", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("balance"));
    }

    @Test
    void anUnknownKindIsAValidationErrorNotAServerError() throws Exception {
        send(post("/api/v1/accounts/manual"), createJson("X", "boat", "1", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("kind"));
    }

    @Test
    void matchesFindsAManualAccountWithTheSameName() throws Exception {
        UUID id = create("Savings", "cash", "10");
        send(post("/api/v1/accounts/matches"), createJson(" savings ", "cash", "0", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Savings"));
        send(post("/api/v1/accounts/matches"), createJson("Other", "cash", "0", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void closeCannotBeInTheFuture() throws Exception {
        UUID id = create("Savings", "cash", "10");
        // 2026-11-28 is tomorrow in Los Angeles.
        send(post("/api/v1/accounts/" + id + "/close"), "{\"closedOn\":\"2026-11-28\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("closedOn"));
        send(post("/api/v1/accounts/" + id + "/close"), "{\"closedOn\":\"2026-11-27\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedOn").value("2026-11-27"));
    }

    @Test
    void closedAccountsCannotBeUpdated() throws Exception {
        UUID id = create("Savings", "cash", "10");
        send(post("/api/v1/accounts/" + id + "/close"), "{\"closedOn\":\"2026-11-27\"}")
                .andExpect(status().isOk());
        send(put("/api/v1/accounts/" + id + "/balance"), "{\"balance\":\"20\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"));
        send(patch("/api/v1/accounts/" + id), "{\"name\":\"New\"}")
                .andExpect(status().isConflict());
        send(post("/api/v1/accounts/" + id + "/reopen"), "{}").andExpect(status().isOk());
        send(patch("/api/v1/accounts/" + id), "{\"name\":\"New\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void patchLeavesAbsentFieldsUnchanged() throws Exception {
        UUID id = create("Savings", "cash", "10");
        send(patch("/api/v1/accounts/" + id), "{\"hidden\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Savings"))
                .andExpect(jsonPath("$.hidden").value(true))
                .andExpect(jsonPath("$.updateReminder").value("monthly"));
    }

    @Test
    void deleteIsRefusedOnceThereIsHistory() throws Exception {
        UUID id = create("Savings", "cash", "10");
        accounts.upsertSnapshot(
                ownerId, id, LocalDate.of(2026, 11, 1), new java.math.BigDecimal("5"));
        mvc.perform(delete("/api/v1/accounts/" + id).with(owner).with(csrf()))
                .andExpect(status().isConflict());
        UUID fresh = create("Fresh", "cash", "1");
        mvc.perform(delete("/api/v1/accounts/" + fresh).with(owner).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        send(put("/api/v1/accounts/" + id + "/balance"), "{\"balance\":\"1\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
        mvc.perform(delete("/api/v1/accounts/" + id).with(owner).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPutsClosedAccountsLastAndHealthCountsOpenOnes() throws Exception {
        UUID closed = create("Aaa", "cash", "1");
        create("Zzz", "cash", "1");
        send(post("/api/v1/accounts/" + closed + "/close"), "{\"closedOn\":\"2026-11-27\"}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Zzz"))
                .andExpect(jsonPath("$[1].name").value("Aaa"))
                .andExpect(jsonPath("$[1].canDelete").value(true));
        mvc.perform(get("/api/v1/accounts/health").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upToDate").value(1))
                .andExpect(jsonPath("$.needsAttention").value(0));
    }
}

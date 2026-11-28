package com.rankedfinance.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestDatabase;
import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// First-run setup (ADR 0010): the setup code from SETUP_CODE creates the owner and signs them in,
// so they can register the first passkey. Once a passkey exists, setup is closed for good.
@SpringBootTest(properties = "app.setup.code=" + SetupTest.SETUP_CODE)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SetupTest {

    static final String SETUP_CODE = "test-setup-code-0123456789";

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired UserCredentialRepository credentials;
    @Autowired UserRepository users;

    // MockMvc requests commit, so each test cleans up after itself.
    @AfterEach
    void deleteEverything() {
        TestDatabase.deleteAllRows(jdbcClient);
    }

    @Test
    void wrongCodeIsRejected() throws Exception {
        setup("not-the-setup-code")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_setup_code"));
        assertThat(userCount()).isZero();
    }

    @Test
    void rightCodeSignsInTheOwner() throws Exception {
        MvcResult result = setup(SETUP_CODE).andExpect(status().isNoContent()).andReturn();
        assertThat(userCount()).isEqualTo(1);

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/webauthn/register/options").session(session).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void setupIsClosedOnceAPasskeyExists() throws Exception {
        credentials.save(TestPasskeys.aPasskey());

        setup(SETUP_CODE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("setup_closed"));
    }

    // Setup stays closed while a passkey exists, so the last one can't be deleted: otherwise anyone
    // with the setup code could claim the instance again.
    @Test
    void cannotDeleteTheLastPasskey() throws Exception {
        signInThroughSetup();
        ImmutableCredentialRecord passkey = TestPasskeys.aPasskey();
        credentials.save(passkey);

        mvc.perform(
                        delete(
                                        "/webauthn/register/"
                                                + passkey.getCredentialId().toBase64UrlString())
                                .with(owner())
                                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "You can't delete your only passkey. Add another one first."));

        assertThat(credentials.findByCredentialId(passkey.getCredentialId())).isNotNull();
        setup(SETUP_CODE).andExpect(jsonPath("$.error").value("setup_closed"));
    }

    // Setup closes once a passkey exists, including for a setup session left over from before
    // (for example when the sign-in right after registration failed).
    @Test
    void leftoverSetupSessionCannotRegisterAnotherPasskey() throws Exception {
        MockHttpSession session = signInThroughSetup();
        credentials.save(TestPasskeys.aPasskey());

        mvc.perform(
                        post("/webauthn/register")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
    }

    // Spring's built-in passkey-management page would offer a delete button that bypasses the app.
    @Test
    void defaultPasskeyPageIsOff() throws Exception {
        signInThroughSetup();

        mvc.perform(get("/webauthn/register").with(owner())).andExpect(status().isNotFound());
    }

    // The setup code unlocks passkey registration only (ADR 0013). Using the app takes a real
    // passkey sign-in, so a browser whose passkey setup failed isn't left signed in without one.
    @Test
    void setupSessionCannotUseTheApp() throws Exception {
        mvc.perform(get("/api/v1/me").session(signInThroughSetup()))
                .andExpect(status().isForbidden());
    }

    // A CSRF token planted before setup must not keep working after it.
    @Test
    void setupClearsTheCsrfToken() throws Exception {
        MvcResult result = setup(SETUP_CODE).andExpect(status().isNoContent()).andReturn();

        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .filteredOn(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .last()
                .asString()
                .contains("Max-Age=0");
    }

    // Retrying setup (for example after a cancelled passkey prompt) reuses the owner row.
    @Test
    void setupCanBeRepeatedBeforeAPasskeyExists() throws Exception {
        setup(SETUP_CODE).andExpect(status().isNoContent());
        setup(SETUP_CODE).andExpect(status().isNoContent());

        assertThat(userCount()).isEqualTo(1);
    }

    @Test
    void ownerEmailMismatchIsReported() throws Exception {
        users.createOwner("someone-else@example.com", "Someone");

        setup(SETUP_CODE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "OWNER_EMAIL doesn't match the owner already on this server."));
    }

    private static RequestPostProcessor owner() {
        return user("owner@example.com").roles(Roles.OWNER);
    }

    private MockHttpSession signInThroughSetup() throws Exception {
        MvcResult result = setup(SETUP_CODE).andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private ResultActions setup(String code) throws Exception {
        return mvc.perform(
                post("/api/v1/setup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"));
    }

    private long userCount() {
        return jdbcClient.sql("select count(*) from users").query(Long.class).single();
    }
}

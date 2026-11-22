package com.rankedfinance.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestcontainersConfiguration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

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

    // MockMvc requests commit, so each test cleans up after itself.
    @AfterEach
    void deleteEverything() {
        jdbcClient.sql("delete from user_credentials").update();
        jdbcClient.sql("delete from user_entities").update();
        jdbcClient.sql("delete from users").update();
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
        credentials.save(aPasskey());

        setup(SETUP_CODE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("setup_closed"));
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

    private static ImmutableCredentialRecord aPasskey() {
        return ImmutableCredentialRecord.builder()
                .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
                .credentialId(Bytes.random())
                .userEntityUserId(Bytes.random())
                .publicKey(new ImmutablePublicKeyCose(new byte[] {1, 2, 3}))
                .signatureCount(0)
                .uvInitialized(true)
                .transports(Set.of())
                .backupEligible(false)
                .backupState(false)
                .attestationObject(new Bytes(new byte[] {1}))
                .attestationClientDataJSON(new Bytes(new byte[] {1}))
                .created(Instant.now())
                .lastUsed(Instant.now())
                .label("test passkey")
                .build();
    }
}

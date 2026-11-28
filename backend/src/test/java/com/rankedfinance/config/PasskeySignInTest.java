package com.rankedfinance.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rankedfinance.TestDatabase;
import com.rankedfinance.TestOwner;
import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.user.OwnerPrincipal;
import com.rankedfinance.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// A real passkey can't run in MockMvc, so the passkey check itself (the relying party) is mocked
// to accept the sign-in; everything after it is the real filter chain. This pins SecurityConfig's
// ownerPasskeySignIn: if it stopped running, the owner would get 403 on every controller.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PasskeySignInTest {

    // Shaped like a browser's navigator.credentials.get() result; the mock ignores its contents.
    private static final String ASSERTION =
            """
            {"id":"AQID","rawId":"AQID","type":"public-key","clientExtensionResults":{},
             "response":{"authenticatorData":"AQID","clientDataJSON":"AQID",
                         "signature":"AQID","userHandle":"AQID"}}
            """;

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JdbcClient jdbcClient;
    @MockitoBean WebAuthnRelyingPartyOperations passkeyOperations;

    @AfterEach
    void deleteEverything() {
        TestDatabase.deleteAllRows(jdbcClient);
    }

    @Test
    void passkeySignInGivesControllersTheOwnersId() throws Exception {
        UUID id = TestOwner.create(users);
        when(passkeyOperations.createCredentialRequestOptions(any()))
                .thenReturn(
                        PublicKeyCredentialRequestOptions.builder()
                                .challenge(Bytes.random())
                                .rpId("localhost")
                                .build());
        when(passkeyOperations.authenticate(any()))
                .thenReturn(
                        ImmutablePublicKeyCredentialUserEntity.builder()
                                .name(TestOwner.EMAIL)
                                .id(Bytes.random())
                                .displayName("Owner")
                                .build());
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/webauthn/authenticate/options").session(session).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/login/webauthn")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ASSERTION))
                .andExpect(status().isOk());

        SecurityContext context =
                (SecurityContext)
                        session.getAttribute(
                                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getPrincipal())
                .isEqualTo(new OwnerPrincipal(id, TestOwner.EMAIL));
        mvc.perform(get("/api/v1/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }
}

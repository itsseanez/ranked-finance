package com.rankedfinance.auth;

import java.time.Instant;
import java.util.Set;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;

/** A stored passkey for tests that need "a passkey exists". The key material is fake. */
final class TestPasskeys {

    private TestPasskeys() {}

    static ImmutableCredentialRecord aPasskey() {
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

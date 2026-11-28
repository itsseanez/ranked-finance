# 0016. Token encryption

- **Status:** Accepted
- **Date:** 2026-11-28

## Context
Plaid access tokens and SimpleFIN access URLs let anyone holding them read the user's bank data, so they can't sit in the database as plain text. The options were the JDK's own `javax.crypto`, a library such as Tink or Jasypt, or a cloud KMS (AWS KMS) from the start. A KMS would break self-hosting, which must work with only environment variables. A library would add a dependency to vet for AGPL compatibility without doing more than the JDK does here.

## Decision
Encrypt with AES-256-GCM from the JDK. The key comes from the `ENCRYPTION_KEY` environment variable (base64 for 32 bytes, made with `openssl rand -base64 32`). Each stored value is `[key version: 1 byte][nonce: 12 random bytes][ciphertext + 16-byte tag]`. The version byte lets a key be rotated later: new values use the new key, and old ones are re-encrypted on read. The code uses a `TokenCipher` interface, so a KMS implementation can replace it. The app starts without a key, but encrypting or decrypting then fails with a clear error. M2b makes the key required when Plaid is configured.

## Consequences
- Self-hosters need one more secret, and it must be backed up with the database. Losing the key means every account has to be re-linked.
- A database leak alone doesn't expose tokens; a leak of the database and the environment does.
- Envelope encryption (a KMS-held master key wrapping per-install data keys) waits for M10, the hosted deployment, behind the same interface.
- Rotating a key needs a small job that re-encrypts stored values; the version byte makes that possible but nothing implements it yet.

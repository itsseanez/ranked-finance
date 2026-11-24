# 0010. Passkey-first sign-in, with a setup code for the first passkey

- **Status:** Accepted; the "setup is unavailable without `SETUP_CODE`" part is superseded by [0012](0012-generated-setup-code-in-logs.md)
- **Date:** 2026-11-22

## Context
Sign-in has to protect live read access to every account a user has. The options:

1. **Email, password, and mandatory two-factor codes, with passkeys added later.** The flow Wealthfront, Fidelity, Schwab, and Wealthsimple use, mostly because they had password accounts before passkeys existed. It requires storing password hashes, password reset by email, a TOTP library, lockouts, and remembered devices. Passwords and one-time codes can be phished: a fake site relays them to the real one.
2. **Passkey-first.** Passkeys are phishing-resistant (the browser only uses them on the domain they were created for), the server stores only public keys, and there's no password to reuse or leak. CISA ranks FIDO/WebAuthn as the strongest form of MFA, and NIST SP 800-63B-4 accepts synced passkeys at AAL2.

Spring Security registers a passkey only for a user who is already signed in, so passkey-first needs a way to create the first session. Self-hosted apps commonly let the first person to register become the owner (Firefly III, Actual Budget), which lets a stranger claim a freshly deployed instance before its owner does. Jenkins avoids that with a generated setup password printed to its logs, but this project never writes secrets to logs.

## Decision
- Sign-in is passkey-first: no passwords and no one-time codes.
- The first passkey is registered after entering a **setup code** that the operator sets in the `SETUP_CODE` environment variable (at least 20 random characters). The correct code creates the owner's `users` row from `OWNER_EMAIL` and `OWNER_DISPLAY_NAME` and signs the owner in, so Spring Security's standard registration endpoints can register the passkey.
- Once any passkey exists, the setup path is closed permanently. Additional (backup) passkeys can be added only from a signed-in session, and each one triggers a sign-in alert (Milestone 7).
- If `SETUP_CODE` isn't set, setup is unavailable; an empty code never matches. The `dev` profile sets a fixed, non-secret code.
- The WebAuthn user entity is linked to the owner by its `name`, which is the owner's email.
- Sign in with Google and other OIDC providers arrives in Milestone 9 as an additional way in. When configured, signing in with the account whose verified email equals `OWNER_EMAIL` can also complete first-run setup. Accounts are linked to the provider's subject identifier in a `user_identities` table, never by email alone.

## Consequences
- No password storage, password reset, or one-time-code handling to build or secure.
- Account recovery is the weakest link and must be designed with the same care: a backup passkey and an email recovery code (Milestone 9).
- A synced passkey is only as secure as the platform account it syncs through (Apple, Google, 1Password).
- Operators must set `SETUP_CODE` before first use; the self-hosting guide documents generating one (for example `openssl rand -base64 24`).
- Supersedes the earlier plan to open passkey registration to anonymous visitors until the first passkey exists.

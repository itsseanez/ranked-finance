# 0013. The setup code unlocks passkey registration only

- **Status:** Accepted
- **Date:** 2026-11-23
- **Supersedes:** the "the correct code ... signs the owner in" part of [0010](0010-passkey-first-sign-in.md)

## Context
Under ADR 0010, the right setup code created the owner and signed them in fully, so they could register their first passkey. If registering the passkey then failed or was cancelled, that browser stayed signed in as the owner for the 30-day session lifetime, with no passkey at all. Only someone with the setup code could get there, but it undercut passkey-first sign-in.

## Decision
- The right setup code opens a **setup session** with the role `ROLE_SETUP`. It can call only the passkey registration endpoints (`POST /webauthn/register/options` and `POST /webauthn/register`) and `/logout`. Everything else requires `ROLE_OWNER`, so the API answers 403.
- After registering, the sign-in page signs in with the new passkey through Spring Security's standard passkey login, which gives `ROLE_OWNER`. Setup takes two passkey prompts: create, then sign in.
- Starting a setup session changes the session ID and clears the CSRF token, so values planted before setup stop working.
- The frontend treats a 403 from `GET /api/v1/me` like a 401 and sends the user to `/login`.

## Consequences
- No browser is ever signed in as the owner without a passkey.
- Real sign-ins all go through Spring Security's passkey login, which rotates the session and CSRF token and publishes authentication events. The sign-in alerts planned for Milestone 7 can listen there.
- One extra passkey prompt during first-time setup.
- If the sign-in after registration fails, the passkey still exists and setup is closed, so the sign-in page is the way back in.

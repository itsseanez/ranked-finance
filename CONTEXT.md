# Ranked Finance glossary

The names this codebase uses for its domain. Code, tests, docs, and reviews use these terms, so a concept has one name everywhere. `docs/design.md` is the product source of truth; this file only pins vocabulary.

## Sign-in

- **Owner**: the one person an install belongs to in v1. Their row in `users` comes from `OWNER_EMAIL` and `OWNER_DISPLAY_NAME`; the database allows only one. Signed in, they have the `OWNER` role.
- **Passkey**: the only way to sign in (ADR 0010). Stored by Spring Security in `user_credentials`. The last passkey can't be deleted, so setup can't reopen.
- **First-run setup**: how the first passkey gets registered on a new install, and the module `auth/FirstRunSetup` that owns every rule about it. Setup is **open** until a passkey exists, then **closed** for good.
- **Setup code**: the secret that unlocks first-run setup. The operator's `SETUP_CODE`, or one the server generates and prints in its startup log (ADR 0012).
- **Setup session**: what the right setup code gives you: a session with the `SETUP` role that can only register a passkey, and only while setup is open (ADR 0013). After registering, you sign in with the new passkey to become the signed-in owner.

# 0012. Generate the setup code when SETUP_CODE isn't set, and print it in the log

- **Status:** Accepted
- **Date:** 2026-11-23
- **Supersedes:** the "If `SETUP_CODE` isn't set, setup is unavailable" part of [0010](0010-passkey-first-sign-in.md)

## Context
ADR 0010 requires the operator to set `SETUP_CODE` before first use, and turns setup off when it's missing. That's one more thing to configure before the app does anything, and a missing setting fails with a dead end ("Setup isn't configured on this server").

Self-hosted tools that guard first-run setup with a secret usually generate it themselves:

- **Jenkins** generates an initial admin password, prints it in the console log between two rows of asterisks, and also writes it to `secrets/initialAdminPassword` in its home directory ([Jenkins docs](https://www.jenkins.io/doc/book/installing/linux/#unlocking-jenkins)).
- **GitLab** generates an initial root password and writes it to `/etc/gitlab/initial_root_password` for 24 hours, but prints it to the log only if you opt in. `GITLAB_ROOT_PASSWORD` sets your own ([GitLab docs](https://docs.gitlab.com/omnibus/installation/)).

A file inside a container is awkward to reach on the hosted deployment (it means exec-ing into a running task), while the log is where an operator already looks (`docker logs`, CloudWatch). CLAUDE.md says secrets never appear in logs, so printing the code there is a deliberate exception.

## Decision
- If `SETUP_CODE` is set (at least 20 characters), it's the setup code, and it's never printed.
- If it isn't set, the server generates a code at startup: 24 random bytes from `SecureRandom`, base64url-encoded (32 characters). It lives only in memory; nothing is written to disk or the database.
- While no passkey exists, the server prints the generated code once at startup in a block between two rows of asterisks, with a line saying where to enter it. Once a passkey exists, nothing is printed.
- Each restart makes a new code, so a code seen in an old log stops working after a restart.
- The setup screen tells the operator where to find the code.
- The `dev` profile keeps its fixed, non-secret code.

## Consequences
- A fresh install works with no setup-related configuration.
- **Anyone who can read the server's logs during the setup window can claim the instance.** That's the same people who can already read its environment variables, so it doesn't widen access much. The window closes for good when the first passkey is registered.
- The code reaches whatever stores the logs (CloudWatch, a log shipper) and stays there after setup closes. It's useless by then: setup is closed, and the code was only ever in memory.
- The "no secrets in logs" rule in CLAUDE.md gets one named exception, this code. The log-output test planned for tokens and account numbers doesn't need to allow anything else.
- `setup_not_configured` can no longer happen, so it's removed from the setup endpoint and the sign-in page.

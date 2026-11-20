# Security Policy

Ranked Finance connects to users' financial accounts (read-only, through Plaid). Security reports are taken seriously.

## Supported versions

Ranked Finance is a hosted app, not a library with releases. Only the latest code on the `main` branch and the currently deployed service receive security fixes.

## Reporting a vulnerability

Please **do not open a public issue** for security problems. Instead, report privately using GitHub's "Report a vulnerability" button on the repository's Security tab (private vulnerability reporting).

Include what you found, how to reproduce it, and what an attacker could do with it. You'll get a reply within 7 days. Please give a reasonable time to fix the issue before disclosing it publicly.

## Scope

In scope: this repository's code and the hosted Ranked Finance service.

Out of scope: Plaid, Anthropic, or AWS themselves (report to them directly), denial-of-service and volume attacks, and social engineering.

## What the app never does

- Moves money, trades, or writes to any financial account.
- Stores bank usernames or passwords (Plaid handles sign-in; the app stores only encrypted access tokens).
- Sells user data.

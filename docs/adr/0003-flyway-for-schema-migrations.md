# 0003. Flyway for schema migrations

- **Status:** Accepted
- **Date:** 2026-11-19

## Context
The schema grows every milestone. It must be identical across local development, tests (Testcontainers), and production, and changes must be reviewable. Hibernate can create and update tables automatically, but it guesses, can't handle renames or data moves, and can silently get things wrong. Liquibase is the main alternative.

## Decision
Use Flyway with plain SQL migration files in `backend/src/main/resources/db/migration`, named `V<n>__description.sql`. Hibernate (if used) never creates or alters tables.

## Consequences
- Every schema change is a reviewed file in git, applied exactly once, in order, on startup.
- Never edit a migration that has already run; fix mistakes with a new migration.
- Migrations contain schema only: no real data, emails, or passwords, since the repo is public.

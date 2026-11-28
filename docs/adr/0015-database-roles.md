# 0015. Separate database roles for the app and for migrations

- **Status:** Accepted
- **Date:** 2026-11-28

## Context
CLAUDE.md requires the app's database user to read and write rows only, with migrations run by a separate schema-owner user. Until now the app and Flyway both connected as the compose `POSTGRES_USER`, which owns the database, and tests connected as the Testcontainers superuser. A SQL injection bug or a stray statement could then drop tables, change the schema, or rewrite `flyway_schema_history`.

Options for creating the restricted role:
- **A migration creates it.** Every install gets the role automatically, but its password would sit in a migration file (or a placeholder that ends up in Flyway's history), the migration user would need `CREATEROLE`, and managed databases (Amazon RDS) restrict role creation in ways a migration can't predict.
- **The operator creates both roles; migrations only grant.** One extra setup step per install, but no password is ever in the repo or the schema history, and each deployment creates roles the way its platform expects.

Spring Boot supports two users directly: setting `spring.flyway.user` makes Flyway open its own connection, while the app's pool uses `spring.datasource.username`.

## Decision
The operator creates two roles: the schema owner, which Flyway uses (`DB_MIGRATION_USER`, locally `rankedfinance`), and the app role (`DB_USER`, locally `rankedfinance_app`), which can log in and nothing more. Migrations never create roles or set passwords. `V4__app_role_grants.sql` grants the app role `select, insert, update, delete` on tables and `usage, select` on sequences, sets the same as default privileges for tables created later, and revokes everything on `flyway_schema_history`. The role name reaches the migration through the `app_role` Flyway placeholder, which defaults to `DB_USER`.

Locally, `db/init/01-app-role.sql` creates the app role with a local-only password when the compose volume is first created. The compose service carries the `org.springframework.boot.ignore` label so Boot's compose support starts it without replacing the app's credentials with the owner's. Tests start the same image as the same owner with the same init script, and connect the app as the restricted role; a test-only script also grants it `TRUNCATE` so tests can empty tables between runs.

## Consequences
- The app can't change the schema or the migration history; tests run as the restricted role, so a missing grant fails the build instead of production.
- Every install has one more setup step. The hosted deployment creates both roles when it provisions the database, and `SELF_HOSTING.md` (Milestone 9) documents both for self-hosters.
- Existing local databases need one `docker compose down -v`, because init scripts run only on an empty volume.
- New migrations need no grants of their own as long as the owner creates the tables (default privileges cover them). A new schema needs its own grants.
- Flyway placeholder replacement is on, so a migration that needs a literal `${` must escape it or turn placeholders off for that file.

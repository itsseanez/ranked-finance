# 0009. PostgreSQL 19, pinned to one major version everywhere

- **Status:** Accepted
- **Date:** 2026-11-21

## Context
The M1 plan named PostgreSQL 17 without recording why, and Spring Initializr generated the test container as `postgres:latest`. When PostgreSQL 19 was released on 2026-11-19, `latest` silently moved tests to 19 while local development stayed on 17. Options considered: 17 (oldest of the three, supported until late 2029), 18 (a year old, generally available on Amazon RDS since November 2025, certified by Flyway), and 19 (just released; RDS has it only in its preview environment, and Flyway doesn't list it yet). No real data exists until the hosted deploy, so changing major versions before then costs only a fresh local database.

## Decision
Use PostgreSQL 19, pinned by major version (`postgres:19`) in `compose.yaml`, `TestcontainersConfiguration`, CI, and the hosted database. Never use `latest`.

## Consequences
- Dev, tests, CI, and production run the same major version, so a new Postgres release can't change what tests check without a reviewed change.
- 19 is newer than Flyway's support list today. Flyway is expected to warn and still migrate; if it fails, fall back to 18 (two image tags and the volume path) until Flyway adds 19.
- Before the hosted deploy, confirm RDS for PostgreSQL 19 is generally available (not preview) and that the Flyway version in use lists 19.
- The official image's data layout changed in 18: the volume mounts at `/var/lib/postgresql`, not `/var/lib/postgresql/data`.
- Upgrading to a later major is a data migration (`pg_upgrade` or dump and restore) once real data exists; record it in a new ADR that supersedes this one.

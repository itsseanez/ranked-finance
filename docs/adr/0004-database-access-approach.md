# 0004. Database access: `JdbcClient` or Spring Data JPA

- **Status:** Proposed
- **Date:** 2026-11-19

## Context
The Milestone 1 plan uses Spring's `JdbcClient`: hand-written SQL mapped to Java records. It is explicit, simple, and makes every query visible, which suits money calculations. Spring Data JPA (Hibernate) is what most Java teams use and what interviews ask about, but adds hidden behavior to learn (lazy loading, the N+1 query problem, entity states). jOOQ was also considered; it's less common in job postings.

Options:
1. `JdbcClient` everywhere (current plan).
2. Spring Data JPA for ordinary reads and writes (users, accounts, plan steps), with `JdbcClient` for heavier calculation queries (net worth history, 14-day rank checks).

## Decision
Not decided yet. Until it is, Milestone 1 proceeds with option 1.

## Consequences
- Option 1: least to learn, explicit SQL, less interview coverage of JPA.
- Option 2: closer to typical Java jobs; more concepts to learn; two data-access styles in one codebase.
- Either way, Flyway owns the schema (ADR 0003) and all SQL is parameterized.

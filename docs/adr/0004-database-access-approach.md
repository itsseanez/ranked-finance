# 0004. Database access with `JdbcClient` and plain SQL

- **Status:** Accepted
- **Date:** 2026-11-22 (proposed 2026-11-19)

## Context
The backend needs one way to read and write PostgreSQL. The data is shallow (a user, their accounts, and each account's transactions and daily snapshots), and the hard queries are reads over date ranges: net worth history, the 14-day rank buffer, median monthly spending, and projections. Writes are mostly simple inserts and updates.

Options considered:
1. **Spring's `JdbcClient`:** hand-written SQL mapped to Java records.
2. **Spring Data JPA (Hibernate):** the most common choice in Spring applications. It maps objects to tables and generates queries, but adds behavior that isn't visible in the code: lazy loading, entity states, and the N+1 query problem.
3. **Spring Data JDBC:** repository interfaces that generate simple queries, without Hibernate. Fewer surprises than JPA, but its own conventions and less documentation.
4. A hybrid: JPA or Spring Data JDBC for simple reads and writes, `JdbcClient` for calculation queries.

## Decision
Use `JdbcClient` with plain SQL everywhere. No JPA, no Spring Data JDBC, one data-access style.

Writing SQL by hand used to be the main cost of this approach; with AI-assisted development that cost is low, while the benefits of explicit SQL remain: every query that touches money, ranks, or the plan is visible in the code, can be run with `EXPLAIN`, and can't be slowed down by hidden queries. It also keeps memory and startup costs down within the self-hosted 1 GB budget.

## Consequences
- **SQL isn't checked at compile time.** A misspelled column fails only when the query runs, so every SQL statement must run in at least one test against real Postgres (Testcontainers). Queries that join, aggregate, cover a date range, or feed money, rank, or plan math get their own `@JdbcTest`; simple single-row statements count as covered when another test runs them.
- **Every query is reviewed for scoping and parameters.** Each query filters by the logged-in user's `user_id` and uses named parameters (`:name`), never string concatenation. The pull request template has a checkbox for it.
- Mapping rows to records is written by hand, which is more code than a generated repository.
- Spring Data JPA, the more common choice in Java teams, isn't used in this codebase.
- Flyway owns the schema either way (ADR 0003).

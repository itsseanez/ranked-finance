# 0006. Feature-based structure on both sides

- **Status:** Accepted
- **Date:** 2026-11-20

## Context
The repo holds a Spring Boot backend and a React frontend, and will grow by feature (Plaid sync, net worth, ranks, plan, AI). Layer-based layouts (`controller/`, `service/`, or `components/`, `hooks/`) scatter one feature across many folders. Common guidance for Spring Boot is package-by-feature with cross-cutting configuration in a `config/` package; for React, bulletproof-react's `app/` + `features/` + shared folders with one-way imports enforced by the linter (Oxlint since [0011](0011-frontend-linting-and-typescript-version.md)).

## Decision
- Monorepo with `backend/` and `frontend/` at the root; repo-wide tooling (Git hooks, commitlint) in a small root `package.json`.
- Backend: one package per feature under `com.rankedfinance`, plus `config/` (cross-cutting configuration) and `common/` (shared types). Tests mirror the main packages.
- Frontend: `src/app/` (providers, router, shell), `src/features/<name>/` using the same feature names as the backend, and shared `components/`, `lib/`, `styles/`. Imports flow shared → features → app; features never import each other, enforced with `import/no-restricted-paths`. `@/` alias for `src/`.

## Consequences
- Everything for one feature is in one backend package and one frontend folder; deleting or changing a feature touches few places.
- Some judgment calls about when code is "shared" enough to move to `common/`, `components/`, or `lib/`.
- If a feature package grows large, it can split internally (for example `api/`, `domain/`, `infrastructure/`) without changing the top-level layout.

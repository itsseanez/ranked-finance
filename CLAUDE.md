# Ranked Finance

Personal finance app built as a ranked game: users climb Iron → Bronze → Silver → Gold → Platinum → Diamond → Master → Grandmaster → Legend (financial independence) by following a plan built from their real accounts. Single user for v1.

## Commands
Planned in the Milestone 1 plan; update this section once they exist (or re-run `/init`).
- `docker compose up -d`: start Postgres (not needed when running the app: `spring-boot-docker-compose` starts it automatically)
- `cd backend && ./mvnw test`: backend tests (Testcontainers needs Docker running)
- `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`: run the API on :8080
- `cd frontend && npm run dev`: run the web app on :5173 (proxies `/api` to :8080)
- `cd frontend && npx vitest run`: frontend tests
- `cd frontend && npm run gen:api`: regenerate TypeScript API types from the backend's OpenAPI spec
- `cd frontend && npm run format && npm run lint`: format with Prettier, then lint with ESLint
- `cd backend && ./mvnw spotless:apply`: format Java (google-java-format, AOSP style)

## Where things are
- `docs/design.md`: source of truth for what gets built. **Read the Ranks sections before touching rank, plan, or money code.**
- `ROADMAP.md`: the milestones in order. `docs/plans/`: the step-by-step plan for the current milestone.
- `docs/adr/`: architecture decision records. Add a new numbered ADR when making a hard-to-reverse technical decision (framework, library, data model, license).
- `design/images/`: UI mockups. Local only (git-ignored); if missing, ask for a screenshot instead of guessing a layout.
- `backend/src/main/java/com/rankedfinance/`: one package per feature (`auth`, `user`, `sync`, `imports`, `categories`, `accounts`, `networth`, `ranks`, `plan`, `ai`, `notifications`) holding its controller, service, repository, and records. `config/` is for cross-cutting configuration only; `common/` for small types several features share. Tests mirror the main packages.
- `frontend/src/`: `app/` (providers, router, shell) → `features/<name>/` (same names as the backend) → shared `components/`, `lib/`, `styles/`. A feature never imports another feature; shared code moves to `components/` or `lib/` (ESLint enforces this). Import with the `@/` alias.
- `frontend/src/lib/api-schema.d.ts` is generated; never edit it.
- `sync/` holds the `DataProvider` interface with one subpackage per provider (`sync/plaid`, `sync/simplefin`). `imports/` parses CSV, OFX/QFX, and QIF files. Nothing outside `sync/` and `imports/` touches provider types; everything else reads the normalized tables.
- New feature? Add the backend package and the matching `features/<name>/` folder together.

## Stack
Java 25, Spring Boot 4.x, Maven, Spring Security passkeys (spring-security-webauthn), Spring JDBC (`JdbcClient`), Flyway, PostgreSQL 19, Testcontainers. React 19, TypeScript, Vite, Tailwind v4, TanStack Query, Vitest. Plaid Java SDK, SimpleFIN over HTTP, Spring AI (Anthropic, OpenAI, OpenAI-compatible). Docker image for self-hosting; AWS for the hosted version.

## Rules that must not break
- **Numbers are computed in code, never by the AI.** The model explains numbers the server computed. It never decides a rank and never edits the plan; it can only propose a suggestion card.
- **Money is `BigDecimal` / `numeric(14,2)`.** No `double` or `float` for money.
- **Rank thresholds are data** (`rank_definitions`), not constants in code. Ranks stack: a rank counts only if every rank below it is also met.
- **Rank steps are generated and locked.** They can't be moved or removed; only personal goals move.
- **Plans may use estimates; ranks use only real numbers.**
- **API types are generated** (`npm run gen:api`). Never hand-write them.
- **The API is versioned:** every endpoint lives under `/api/v1/`. It becomes public in 1.0 (API tokens), so a breaking change means `/api/v2/`, never changing v1 in place. Errors use RFC 9457 Problem Details; never put secrets or stack traces in them.
- **Module boundaries are tested:** `ModularityTest` (Spring Modulith) fails on package cycles or reaching into another feature's internals. Fix the design, don't suppress the check.
- **Plaid: Sandbox only** until Milestone 10 (the Trial plan has 10 lifetime connections). Plaid sync is webhook-driven, SimpleFIN is a daily pull; no "sync now" or forced refresh.
- **Providers are swappable.** Ranks, plan, and AI code read only the normalized tables (`connections`, `accounts`, `transactions`, `balance_snapshots`), never Plaid or SimpleFIN fields. Data only some providers supply (APRs, contributions) always has a fallback. See ADR 0007.
- **AI is optional and swappable.** Call it only through `AiClient`. The app must work with no AI configured; nothing that affects rank, plan, or balances depends on an AI response. See ADR 0008.
- **Categories come from one pipeline** for every source. An AI-suggested category doesn't count as spending until the user confirms it. Matched transfers are never spending or income.
- **US and Canada only** in v1. Each install has a home currency; each account keeps its own. Convert with that day's stored rate; never assume 1:1 when a rate is missing.
- **Every source counts the same** toward net worth and ranks. No "self-reported" or "verified" labels.
- **Correct spending:** pending transactions never count and are replaced when they post; refunds are negative spending, never income; dates are computed in the user's time zone, never the server's.
- **Accessible and locale-ready:** keyboard-usable with visible focus, WCAG AA contrast, a data table behind every chart, color never the only signal. Format money and dates with `Intl`; keep UI text in one place per feature.
- **Speed is a requirement:** pages under 500 ms with 10 years of data (the demo seed) in 1 GB of RAM. Every new table's migration adds the indexes its queries need. Transaction lists are paginated on the server and virtualized on screen. Never sum all history per request; read snapshots or precomputed totals.
- **Self-hostable.** All config comes from environment variables. AWS services are for the hosted deployment only; the code never requires them.
- **Formatting is automatic.** Prettier (frontend) and Spotless (backend) own formatting; ESLint is for code quality only. Don't hand-format or add style rules to ESLint. Hooks and CI must pass; never use `--no-verify`.
- **Schema changes are new Flyway files.** Never edit a migration that has already run; never let Hibernate create tables.

## Security
- Secrets (Plaid keys and access tokens, SimpleFIN access URLs, the encryption key, AI API keys) never appear in the repo, logs, errors, or API responses. Config reads them from environment variables (AWS Secrets Manager or KMS only on the hosted deployment).
- Verify every Plaid webhook signature (`Plaid-Verification` JWT); reject failures.
- API tokens are read-only in v1, shown once, and stored only as a hash.
- Uploaded import files are parsed in memory, size-limited, and never stored or logged; only the resulting transactions are saved.
- Every query is scoped to the logged-in user's `user_id`. SQL is always parameterized.
- The app's database user reads and writes rows only; migrations run as a separate schema-owner user.
- Migrations and seed data contain no real data, emails, or passwords.
- Never log tokens or full account numbers (a test checks log output).
- New dependencies must be AGPL-compatible (MIT, Apache-2.0, BSD, LGPL; no proprietary or SSPL).

## Commits
[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/): `type(scope): description`, lowercase, imperative, no period, under 100 characters.
- Types: `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `build`, `ci`, `chore`, `style`.
- Scopes (optional): backend feature packages (`auth`, `user`, `sync`, `imports`, `categories`, `accounts`, `networth`, `ranks`, `plan`, `ai`, `notifications`), plus `web`, `db`, `deps`, `ci`, `docs`.
- Breaking changes: `!` before the colon (`feat(ranks)!: ...`) or a `BREAKING CHANGE:` footer.
- Examples: `feat(ranks): add 14-day drop rule` · `fix(sync): reject Plaid webhooks with invalid signatures` · `docs(adr): add 0005 webhook retries`
- Fill in the PR template's checklist (`.github/pull_request_template.md`).
- Work on a branch, one PR per task; `main` is protected and squash-merged, so the PR title must be a valid Conventional Commit. release-please turns the history into `CHANGELOG.md` and version tags; never edit the changelog or bump versions by hand.
- Branch names follow [Conventional Branch](https://conventional-branch.github.io/): `<type>/<description>`, lowercase kebab-case, 2–5 words, named after the work, never a task or milestone number. Types: `feature/` (new functionality), `bugfix/`, `hotfix/` (urgent fix to the deployed app), `release/`, `chore/` (docs, dependencies, config, CI). Examples: `feature/user-repository` · `feature/passkey-login` · `bugfix/registration-gate-csrf` · `chore/update-spotless`. Delete the branch after merging.
- commitlint checks every message (a local `commit-msg` hook and CI); don't bypass it with `--no-verify`.

## Testing
Test what matters, and keep it fast.
- **Test first (TDD)** for: money math; rank rules (14-day buffer, stacking, drops, credit-card-as-debt); plan projections and payoff order; auth and security (registration gate, CSRF, 401s); Plaid webhook verification; token encryption; no tokens in logs; duplicate-account matching; import parsing and duplicate-transaction detection (CSV, OFX, QIF fixtures, including odd date formats and debit/credit columns); the category order (user, user rule, starter rule, provider, AI), transfer matching, pending-to-posted replacement, refunds, time-zone date boundaries, and currency conversion. Frontend behavior that protects the user (auth redirects, sign-in error messages) counts as auth.
- **No tests** for UI layout and styling, page stubs, simple CRUD and plumbing, getters/setters, or copy. Check those by running the app against the mockups.
- One smoke test per app that it starts and migrations run. Every bug gets a failing test that reproduces it before the fix.
- Tricky logic lives in pure classes: data and the date go in (pass a `LocalDate` or inject a `Clock`; never call `now()` inside), a result comes out. No database, no Spring context.
- Lightest test that works: plain JUnit for logic; slice tests (`@WebMvcTest`, `@JdbcTest`) for one layer; `@SpringBootTest` only when the full app is needed. One shared Testcontainers Postgres.
- Assert on results, not on which methods were called. Mock only external services (Plaid, SimpleFIN, the AI provider), at the `DataProvider` and `AiClient` interfaces.
- Small test-data builders so each test sets only what it cares about (build the account/snapshot builders before the first rank tests in M4).
- Exploring an unfamiliar API (Plaid Sandbox, SimpleFIN's demo token)? Spike without tests, then rebuild what you keep test-first.

## Design tokens
Accent #5B3FD6 (tint #F1EEFC) · gain #15803D · debt #B91C1C · warning #A16207 · text #0A0A0A · page #FAFAFA · card #FFFFFF · border #E3E3E3 · Inter. Light / Dark / Match system. Sticky sidebar (Home, Plan, Money, Ranks) on desktop; Ask, bell, gear top right; phone width uses a top bar with a menu, never bottom tabs. Net worth is always first on Home.

## How to work
- The maintainer knows JavaScript/React better than Java: explain a Java or Spring concept in a sentence the first time it appears.
- If `docs/design.md` doesn't answer a product question, ask instead of guessing, then record the answer there.

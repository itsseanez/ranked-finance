# Milestone 1: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A monorepo where the owner logs in with a passkey and sees the empty desktop app shell (sidebar, top bar, page stubs), with backend and frontend tests running in CI.

**Architecture:** A Spring Boot API (`backend/`) on PostgreSQL with Flyway migrations, and passkey login through Spring Security's built-in WebAuthn support, persisted with its JDBC repositories, using a session cookie. A React SPA (`frontend/`) talks to `/api/**` through the Vite dev proxy, and its API types are generated from the backend's OpenAPI spec. Backend code is organized by feature (`auth`, `user`, later `sync`, `imports`, `ranks`, and so on), not by layer.

**Tech Stack:** Java 25, Spring Boot 4.x (latest stable when you start; 4.1.1 as of Nov 2026), Maven, spring-security-webauthn, Spring JDBC, Flyway, PostgreSQL 19, Testcontainers, springdoc-openapi; Node 22, Vite, React 19, TypeScript, Tailwind CSS v4, TanStack Query, React Router, Vitest, Testing Library, openapi-typescript; GitHub Actions.

**Testing:** follow the Testing section of `CLAUDE.md`: test first for money, ranks, plan math, auth and security; no tests for layout, stubs or plumbing.

**Spec:** `docs/design.md` — Security and hosting, Navigation, Data model (users), Gaps closed (passkey recovery comes in M9).

## Global Constraints

- Every domain table carries `user_id uuid not null references users(id)`. Spring Security's own WebAuthn tables are the exception; they key on the WebAuthn user entity, which maps to `users.id`.
- Money will be `BigDecimal` / Postgres `numeric(14,2)`. M1 has no money columns, but don't introduce `double` anywhere.
- Single user: the passkey registration endpoints are open only while no credential exists, or to an already-logged-in user (for a backup passkey).
- Session cookie: `HttpOnly`, `Secure` (except in local dev over http), `SameSite=Lax`, 30-day idle timeout. CSRF on for all state-changing requests; the SPA reads the `XSRF-TOKEN` cookie and sends `X-XSRF-TOKEN`.
- Theme tokens: accent `#5B3FD6`, accent tint `#F1EEFC`, gain `#15803D`, debt `#B91C1C`, warning `#A16207`, text `#0A0A0A`, page `#FAFAFA`, card `#FFFFFF`, border `#E3E3E3`. Font: Inter.
- Navigation: the sidebar lists Home, Plan, Money, Ranks (in that order) plus an account-health line at the bottom. Top right: Ask, bell with unread count, gear. At 900px and up, the sidebar is sticky (`position: sticky; top: 0; height: 100vh`) while the main page scrolls. Below 900px, a top bar with a menu button replaces the sidebar. No bottom tabs.
- App name: **Ranked Finance** in the UI ("Road to Legend" is the rank feature, not the app). Repo `ranked-finance`, Java package `com.rankedfinance`.

## Review Focus

1. **A stranger opens the deployed URL before or after the owner registers.** They must not be able to register a passkey and get in. Pinned in Task 3: `anonymousCannotRequestRegistrationOptions`, `wrongCodeIsRejected`, `setupIsClosedOnceAPasskeyExists`, and `emptyCodeNeverMatches`.
2. **The session expires while the app is open.** The next API call returns 401, and the app should send the user to `/login` rather than show a blank or broken page. Pinned in Task 6: `redirectsToLoginOn401`.
3. **POST without a CSRF token** (a forged request from another site) must be rejected even with a valid session. Pinned in Task 3: `postWithoutCsrfIsForbidden`.
4. **Phone-width browser.** The shell must still be usable: the menu opens and lists every page. Checked by hand against the mockup at 390px in Task 5, Step 4 (UI layout isn't unit-tested).
5. **Browser without passkey support, or the user cancels the prompt.** Show a plain message, not an unhandled promise error. Pinned in Task 6: `showsMessageWhenPasskeyCancelled`.

---

## File Structure

Both halves are organized **by feature**, with matching names (`auth`, `user`, `plan`, `ranks`, …), so the code for one feature is easy to find on each side. See ADR 0006.

```
ranked-finance/
  README.md  CLAUDE.md  SECURITY.md  ROADMAP.md  LICENSE
  compose.yaml                       Postgres 19 for local dev (Docker's preferred file name)
  package.json                       repo-level dev tools only: husky, lint-staged, commitlint
  commitlint.config.mjs
  .husky/                            commit-msg, pre-commit hooks
  .github/workflows/ci.yml           backend, frontend, commitlint jobs
  docs/                              design.md, plans/, adr/
  design/                            local mockups (git-ignored, not committed)

  backend/
    pom.xml
    src/main/java/com/rankedfinance/
      RankedFinanceApplication.java  root package, so component scanning finds every feature
      config/                        cross-cutting configuration only
        SecurityConfig.java          filter chain, webAuthn, CSRF, session
      common/                        small shared types used by several features (empty in M1)
      auth/                          one package per feature: controller, service,
        SetupController.java           repository, and records live together
      user/
        User.java                    record
        UserRepository.java          JdbcClient-based
        MeController.java            GET /api/v1/me
      (later: sync/, imports/, accounts/, networth/, ranks/, plan/, ai/, notifications/)
    src/main/resources/
      application.yaml, application-dev.yaml
      db/migration/V1__users.sql, V2__single_owner.sql, V3__webauthn.sql
    src/test/java/com/rankedfinance/ tests mirror the main packages
      TestcontainersConfiguration.java
      ApplicationSmokeTest.java
      config/SecurityTest.java
      user/UserRepositoryTest.java

  frontend/
    package.json, vite.config.ts, tsconfig.json, eslint.config.js, index.html
    src/
      main.tsx                       entry point
      app/                           application shell: wires features together
        App.tsx                      providers (TanStack Query) + router
        router.tsx                   routes → feature pages
        shell/                       AppShell, Sidebar, TopBar, PhoneBar
      features/                      one folder per feature; features never import each other
        auth/                        passkey.ts, LoginPage.tsx, RequireAuth.tsx, use-me.ts (+ tests)
        home/HomePage.tsx
        plan/PlanPage.tsx
        money/MoneyPage.tsx
        ranks/RanksPage.tsx
        settings/SettingsPage.tsx
        notifications/NotificationsPage.tsx
      components/                    shared UI used by 2+ features (empty in M1)
      lib/
        api-client.ts                fetch wrapper (CSRF, 401 handling) (+ test)
        api-schema.d.ts              generated by openapi-typescript; never edit
      styles/theme.css               Tailwind v4 @theme tokens
      test/setup.ts                  Vitest + Testing Library setup
```

Import rules (frontend): `lib`, `components`, `styles` → may be imported by anyone; `features/*` → may import shared code, never another feature; `app/` → may import features. Use the `@/` alias for `src/` (`@/lib/api-client`). Something two features need moves to `components/` or `lib/`.

---

### Task 1: Backend scaffold, Postgres, Flyway, CI

**Files:**
- Create: `compose.yaml`, `backend/pom.xml`, `backend/src/main/java/com/rankedfinance/RankedFinanceApplication.java`, `backend/src/main/resources/application.yaml`, `backend/src/main/resources/db/migration/V1__users.sql`, `.github/workflows/ci.yml`
- Test: `backend/src/test/java/com/rankedfinance/TestcontainersConfiguration.java`, `ApplicationSmokeTest.java`

**Interfaces:**
- Produces: the `users` table (below); `TestcontainersConfiguration` (generated by Initializr when Testcontainers is ticked, alongside `TestRankedFinanceApplication`; keep both names): a `@TestConfiguration` with a `@ServiceConnection PostgreSQLContainer<?>` bean, image `postgres:19`, that every later integration test imports.

- [ ] **Step 0: Create the repo:** `gh repo create ranked-finance --public --license agpl-3.0 --clone`. In the repo's Settings → Code security, turn on secret scanning, push protection, private vulnerability reporting, Dependabot alerts and security updates, and CodeQL default setup (code scanning) (SECURITY.md is already written). Add `.gitignore` entries for `.env*`, `application-local.yaml`, `design/` (local mockups), and `.vscode/*` with `!.vscode/extensions.json` (so only the shared extension list is committed) before the first commit, then unzip the mockups into `design/`. Add a branch ruleset on `main`: changes arrive through pull requests, the CI checks must pass, merges are squash-only (so the PR title becomes the commit message and commitlint checks it), and force-pushes are blocked. Working solo, you still open a PR per task; it's where CI runs and where a review happens. Also add `.editorconfig` (UTF-8, LF, final newline, 2-space indent except 4 for Java), `.nvmrc` (Node 22), `.sdkmanrc` (Java 25 Temurin), and `.env.example` listing every environment variable with a fake value.
- [ ] **Step 1: Generate the project** with Spring Initializr: Maven, Java 25, latest Spring Boot 4.x, group `com.rankedfinance`, artifact `backend`. Dependencies: Spring Web, Spring Security, JDBC API, Flyway Migration, PostgreSQL Driver, Validation, Testcontainers, Spring Boot DevTools. Add `org.flywaydb:flyway-database-postgresql`.
- [ ] **Step 2: Write the failing test** `ApplicationSmokeTest.contextLoadsAndUsersTableExists`: `@SpringBootTest @Import(TestcontainersConfiguration.class)`; assert `jdbcClient.sql("select count(*) from users").query(Long.class).single()` equals `0L`.
- [ ] **Step 3: Run it to make sure it fails.** Run: `cd backend && ./mvnw test -Dtest=ApplicationSmokeTest`. Expected: FAIL, relation "users" does not exist.
- [ ] **Step 4: Write `V1__users.sql`:**

```sql
create table users (
  id                uuid primary key,
  email             text not null unique,
  display_name      text not null,
  birth_year        int,
  sex               text check (sex in ('male','female')),
  marital_status    text check (marital_status in ('never_married','married','divorced','widowed','separated')),
  living_situation  text check (living_situation in ('alone','partner','parents','other')),
  country           text,
  time_zone         text not null default 'America/New_York',
  home_currency     text not null default 'USD' check (home_currency in ('USD','CAD')),
  created_at        timestamptz not null default now()
);
```
(The About-you fields are all nullable; null means "Prefer not to say." `time_zone` is an IANA name; first-time setup (Milestone 7) sets it from the browser's zone.)
- [ ] **Step 5: Run the test to make sure it passes.** Same command. Expected: PASS.
- [ ] **Step 6: Add `compose.yaml`** (service `db`, `postgres:19`, port 5432, db/user/password `rankedfinance`) and `application.yaml`. Every setting a deployment changes reads an environment variable with a local-dev default, using Spring's `${VAR:default}` placeholder (like `process.env.VAR ?? 'default'`): `spring.datasource.url: ${DB_URL:jdbc:postgresql://localhost:5432/rankedfinance}`, `spring.datasource.username: ${DB_USER:rankedfinance}`, `spring.datasource.password: ${DB_PASSWORD:rankedfinance}`. Fixed settings stay literal: `spring.flyway.enabled: true`, `server.servlet.session.timeout: 30d`. Nothing assumes AWS.
- [ ] **Step 6a: Start Postgres automatically in development.** Add `spring-boot-docker-compose` as a development-only dependency (`<optional>true</optional>`, so it's left out of the packaged app). Because `compose.yaml` sits at the repo root and the app runs from `backend/`, set `spring.docker.compose.file: ../compose.yaml` and `enabled: true` in `application-dev.yaml`, with `spring.docker.compose.enabled: false` in `application.yaml` so other profiles never look for a compose file. Give the `db` service a `pg_isready` healthcheck, which Spring Boot's docs recommend for knowing when the container is ready. Running the app now starts `compose.yaml` and stops it afterwards, so there's no separate `docker compose up` step to forget. Tests keep using Testcontainers.
- [ ] **Step 6b: Health check.** Add `spring-boot-starter-actuator`, expose only the `health` endpoint (`management.endpoints.web.exposure.include: health`, details hidden), and permit `/actuator/health` without login in Task 3's security config. Docker's healthcheck uses it later.
- [ ] **Step 6c: Module boundaries and error format.** Add `spring-modulith-starter-core` and `spring-modulith-starter-test`, and a test `ModularityTest.verifiesModuleStructure` calling `ApplicationModules.of(RankedFinanceApplication.class).verify()`: it fails the build if feature packages form a cycle or reach into another feature's internal subpackages, the backend twin of the frontend's ESLint import rule. Turn on RFC 9457 error responses (`spring.mvc.problemdetails.enabled: true`) so every API error has one JSON shape, and keep logs plain text by default: deployments (the Docker image in Milestone 9, the hosted deploy in Milestone 10) turn on structured JSON logs with the environment variable `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`. A profile can't reliably unset a base property, so JSON-by-default with a dev override doesn't work.
- [ ] **Step 6d: Add Spotless to `pom.xml`** (`com.diffplug.spotless:spotless-maven-plugin`, latest) with `googleJavaFormat` in `AOSP` style (4-space indent), `removeUnusedImports`, and `importOrder`, and bind the `check` goal to the `verify` phase so CI fails on unformatted code. Run `./mvnw spotless:apply` once, then `./mvnw verify` passes. Editors can run the same formatter with `--aosp`; the hook and CI enforce it either way.
- [ ] **Step 7: Add the `backend` job to `ci.yml`:** ubuntu-latest, `actions/setup-java` with Temurin 25, run `./mvnw -B verify` in `backend/`. Testcontainers works on GitHub-hosted runners without extra setup.
- [ ] **Step 8: Commit.** `git add -A && git commit -m "build: scaffold backend with Postgres, Flyway, and CI"`

### Task 2: User record and repository

**Files:**
- Create: `backend/src/main/java/com/rankedfinance/user/User.java`, `UserRepository.java`
- Test: `backend/src/test/java/com/rankedfinance/user/UserRepositoryTest.java`

**Interfaces:**
- Consumes: `users` table, `TestcontainersConfiguration` (Task 1).
- Produces:
  - `record User(UUID id, String email, String displayName, Integer birthYear, String sex, String maritalStatus, String livingSituation, String country)`
  - `UserRepository` (`@Repository`, uses `JdbcClient`):
    - `Optional<User> findById(UUID id)`
    - `Optional<User> findByEmail(String email)`
    - `User createOwner(String email, String displayName)` — inserts with `UUID.randomUUID()`; throws `IllegalStateException("owner already exists")` if any user row exists.
    - `long count()`

- [ ] **Step 1: Write the failing test** (a security rule: there is only ever one owner; plain finds and inserts get no tests of their own).
  - `secondOwnerIsRejected`: `createOwner("sean@example.com","Owner")` succeeds; a second `createOwner` throws `IllegalStateException`; `count()` stays `1`.
- [ ] **Step 2: Run it to make sure it fails.** Run: `./mvnw test -Dtest=UserRepositoryTest`. Expected: compilation failure (no `UserRepository`).
- [ ] **Step 3: Implement `User` and `UserRepository`.** Map rows with a `RowMapper` lambda; `createOwner` checks `count() > 0` first, in the same `@Transactional` method.
- [ ] **Step 4: Run the test to make sure it passes.** Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "feat(user): user record and repository"`

### Task 3: Passkey login, first-run setup code, CSRF, session

See ADR 0010. Spring Security registers passkeys only for a signed-in user, so the first session comes from a setup code the operator sets in `SETUP_CODE`.

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__webauthn.sql`, `backend/src/main/java/com/rankedfinance/config/SecurityConfig.java`, `auth/SetupController.java`, `auth/OwnerUserDetailsService.java`, `auth/SetupProperties.java`
- Modify: `backend/pom.xml` (add `org.springframework.security:spring-security-webauthn`), `user/UserRepository.java` (add `findByEmail`), `application.yaml` (add `app.owner.email: ${OWNER_EMAIL:owner@example.com}`, `app.owner.display-name: ${OWNER_DISPLAY_NAME:Owner}`, `app.setup.code: ${SETUP_CODE:}`, `app.webauthn.rp-id: ${WEBAUTHN_RP_ID:localhost}`, `app.webauthn.origins: ${WEBAUTHN_ORIGINS:http://localhost:5173,http://localhost:8080}`; every install sets its own domain here), `application-dev.yaml` (`app.setup.code: dev-setup-code-not-secret`, `server.servlet.session.cookie.secure: false`)
- Test: `backend/src/test/java/com/rankedfinance/config/SecurityTest.java`, `auth/SetupTest.java`, `auth/SetupNotConfiguredTest.java`, `user/UserRepositoryTest.java` (one new test)

**Interfaces:**
- Consumes: `UserRepository.createOwner`, `UserRepository.count` (Task 2).
- Produces:
  - `UserRepository.findByEmail(String email): Optional<User>`.
  - Spring Security's endpoints: `POST /webauthn/register/options` and `POST /webauthn/register` (signed-in users only, Spring's default), `POST /webauthn/authenticate/options`, `POST /login/webauthn`, `POST /logout` (returns `204`).
  - `POST /api/v1/setup` with body `{"code": "..."}`, open without a session, CSRF required:
    - `403 {"error":"setup_closed"}` if any passkey exists, whatever the code.
    - `403 {"error":"setup_not_configured"}` if `app.setup.code` is blank. An empty code never matches.
    - `401 {"error":"invalid_setup_code"}` if the code doesn't match (constant-time comparison with `MessageDigest.isEqual`).
    - Otherwise: creates the owner row from `app.owner.*` if none exists, signs the owner in (new session ID, security context saved to the session), and returns `204`.
  - `SetupProperties` (`@ConfigurationProperties("app.setup")`, `@Validated`): when the code is set, it must be at least 20 characters, or the app refuses to start.
  - `OwnerUserDetailsService`: loads a user by email (the WebAuthn user entity's `name`) with authority `ROLE_OWNER`. Spring's passkey login uses it.
  - Every `/api/**` path except `/api/v1/setup` requires authentication and returns `401` (not a redirect) without a session.

- [ ] **Step 1: Write the failing tests.** `SecurityTest`, `SetupTest`, and `SetupNotConfiguredTest` use `@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)` and delete from `user_credentials`, `user_entities`, and `users` after each test, since MockMvc requests commit.
  - `SecurityTest.apiRequiresAuth`: `GET /api/v1/me` without a session → `401`.
  - `SecurityTest.anonymousCannotRequestRegistrationOptions`: `POST /webauthn/register/options` with CSRF and no session → refused with a 4xx (Spring's registration filter answers `400` before the `401` entry point runs), and no `user_entities` row is created.
  - `SecurityTest.postWithoutCsrfIsForbidden`: with `@WithMockUser`, `POST /logout` without a CSRF token → `403`.
  - `SetupTest.wrongCodeIsRejected`: → `401 invalid_setup_code`, and no `users` row.
  - `SetupTest.rightCodeSignsInTheOwner`: → `204` and one `users` row; with the returned session, `POST /webauthn/register/options` → `200`.
  - `SetupTest.setupIsClosedOnceAPasskeyExists`: save a credential through `UserCredentialRepository` (built with `ImmutableCredentialRecord.builder()`), then the right code → `403 setup_closed`.
  - `SetupNotConfiguredTest.emptyCodeNeverMatches`: with `app.setup.code` blank, `{"code":""}` → `403 setup_not_configured`.
  - `UserRepositoryTest.findsTheOwnerByEmail`.
- [ ] **Step 2: Run them to make sure they fail.** Run: `./mvnw test -Dtest='SecurityTest,Setup*Test,UserRepositoryTest'`. Expected: compilation failure (no `spring-security-webauthn`, no `findByEmail`).
- [ ] **Step 3: Write `V3__webauthn.sql`** from Spring Security's `user_entities` and `user_credentials` schema (same table and column names, since `JdbcPublicKeyCredentialUserEntityRepository` and `JdbcUserCredentialRepository` query them), using `bytea` where Spring's default script uses `blob`, which Postgres doesn't have.
- [ ] **Step 4: Implement `SecurityConfig`:** `.webAuthn(w -> w.rpName("Ranked Finance").rpId(rpId).allowedOrigins(origins))`; beans `JdbcPublicKeyCredentialUserEntityRepository` and `JdbcUserCredentialRepository` over `JdbcOperations`; CSRF for single-page apps (the `XSRF-TOKEN` cookie readable by JavaScript, the token sent back in `X-XSRF-TOKEN`, as in Spring Security's "Single-Page Applications" CSRF section); a `401` entry point for unauthenticated requests; `/api/v1/setup` and `/actuator/health` permitted; `204` on logout; no form login and no HTTP Basic. Session cookie: `server.servlet.session.cookie.same-site: lax`, `http-only: true`, `secure: true` (overridden to `false` in `application-dev.yaml`).
- [ ] **Step 5: Implement `findByEmail`, `OwnerUserDetailsService`, `SetupProperties`, and `SetupController`.**
- [ ] **Step 6: Run the full suite.** Run: `./mvnw verify`. Expected: all tests pass.
- [ ] **Step 7: Commit.** `git commit -am "feat(auth): passkey-first sign-in with first-run setup code"`

The browser half (entering the setup code, creating the passkey, signing in) is checked end to end in Task 6, once the login screen exists.

### Task 4: `GET /api/v1/me`, OpenAPI spec, generated frontend types

**Files:**
- Create: `backend/src/main/java/com/rankedfinance/user/MeController.java`, `user/MeResponse.java`
- Modify: `backend/pom.xml` (add `org.springdoc:springdoc-openapi-starter-webmvc-api` 3.x, the line built for Spring Boot 4; spec only, no Swagger UI page), `application.yaml` (`springdoc.api-docs.enabled: false`), `application-dev.yaml` (`springdoc.api-docs.enabled: true`), `SecurityConfig` (permit `/v3/api-docs/**`, which exists only in dev)
- Test: `backend/src/test/java/com/rankedfinance/user/MeControllerTest.java`: one end-to-end check that signs in through first-run setup and calls `/api/v1/me`, standing in for a manual check until the login screen exists in Task 6. The 401 without a session is covered by `SecurityTest.apiRequiresAuth` (Task 3).

**Interfaces:**
- Consumes: `UserRepository.findByEmail` (Task 3); the signed-in principal's name is the owner's email (Task 3).
- Produces:
  - `GET /api/v1/me` → `200` with `record MeResponse(UUID id, String displayName, String email)`; `404` if no row, which shouldn't happen.
  - `GET /v3/api-docs` in the `dev` profile only.
  - The npm script `gen:api` (Task 5) reads `http://localhost:8080/v3/api-docs` and writes `frontend/src/lib/api-schema.d.ts`.

- [ ] **Step 1: Write `MeControllerTest.returnsTheSignedInOwner`, `MeResponse`, and `MeController`.**
- [ ] **Step 2: Run the suite.** Run: `./mvnw verify`. Expected: all tests pass. Then start the backend with the `dev` profile and open `http://localhost:8080/v3/api-docs`: it lists `/api/v1/me` and `/api/v1/setup`.
- [ ] **Step 3: Commit.** `git commit -am "feat(user): add /api/v1/me and OpenAPI spec"`

### Task 5: Frontend scaffold and app shell

**Files:**
- Create: everything under `frontend/` listed in File Structure except `features/auth/*` and `lib/api-client.ts`
- Modify: `.github/workflows/ci.yml` (add the `frontend` and `commitlint` jobs)
- Create: `package.json` (repo root), `commitlint.config.mjs`, `.husky/commit-msg`, `.husky/pre-commit`, `frontend/.prettierrc.json`, `frontend/.prettierignore`, `.vscode/extensions.json`
- Test: none (UI layout; checked against the mockup in Step 4)

**Interfaces:**
- Consumes: `GET /api/v1/me` types from `schema.d.ts` (Task 4).
- Produces:
  - Routes: `/` Home, `/plan`, `/money`, `/ranks`, `/settings`, `/notifications`, `/login` (outside the shell).
  - `<AppShell>` (in `src/app/shell/`) renders `<Sidebar>` (≥900px) or `<PhoneBar>` (<900px), plus `<TopBar>` and an `<Outlet>`.
  - `<Sidebar activePath>`: links in the order Home, Plan, Money, Ranks; the active link uses the accent tint background and accent text; footer slot `healthLine` (static text "No accounts linked yet" for now).
  - `<TopBar unreadCount>`: Ask button (`aria-expanded`, toggles an empty right-side panel for now), bell linking to `/notifications` with the badge hidden when `unreadCount === 0`, gear linking to `/settings`.
  - Each page stub renders an `<h1>` with its name.

- [ ] **Step 1: Scaffold** with `npm create vite@latest frontend -- --template react-ts`; add `tailwindcss @tailwindcss/vite @tanstack/react-query react-router`, and dev deps `vitest @testing-library/react @testing-library/user-event jsdom openapi-typescript`. Proxy `/api`, `/webauthn`, `/login`, `/logout` to `http://localhost:8080` in `vite.config.ts`. Script `"gen:api": "openapi-typescript http://localhost:8080/v3/api-docs -o src/lib/api-schema.d.ts"`.
- [ ] **Step 1b: Prettier and ESLint.** Add dev dependencies `prettier` (exact version, per Prettier's install docs), `eslint-config-prettier`, and `eslint-plugin-import`. Keep the template's `eslint.config.js` (flat config) and add `eslint-config-prettier` as the **last** entry so it turns off ESLint's formatting rules; use `tseslint.configs.recommended` (or `strict`). Do **not** add `eslint-plugin-prettier`: Prettier formats, ESLint checks code quality. Add `.prettierrc.json` (`{}`, Prettier defaults) and `.prettierignore` (`dist`, `coverage`, `src/lib/api-schema.d.ts`). Add the `@/` path alias for `src/` in `tsconfig.json` and `vite.config.ts`. Enforce the import rules from File Structure with `eslint-plugin-import`'s `import/no-restricted-paths` (one zone per feature, as bulletproof-react does): a feature may not import from another feature, and `lib`/`components` may not import from `features` or `app`. Scripts: `"lint": "eslint ."`, `"format": "prettier --write ."`, `"format:check": "prettier --check ."`.
- [ ] **Step 1c: Recommended VS Code extensions.** Add `.vscode/extensions.json` at the repo root with `"recommendations": ["esbenp.prettier-vscode", "dbaeumer.vscode-eslint", "vscjava.vscode-java-pack", "josevseb.google-java-format-for-vs-code"]`. Commit only this file from `.vscode/`, not `settings.json`, so personal editor settings stay out of the repo.
- [ ] **Step 2: Write `src/styles/theme.css`** with `@import "tailwindcss";` and an `@theme` block defining `--color-accent`, `--color-accent-tint`, `--color-gain`, `--color-debt`, `--color-warn`, `--color-ink`, `--color-page`, `--color-card`, `--color-line`, and `--font-sans: Inter, system-ui, sans-serif` using the Global Constraints values.
- [ ] **Step 3: Implement `app/shell/` (`AppShell`, `Sidebar`, `TopBar`, `PhoneBar`), `app/router.tsx`, and the page stubs in each `features/<name>/` folder,** matching `design/images/01-desktop-home.png` (local, git-ignored) for spacing and colors: sidebar 220px, white, right border; nav items 44px tall with 10px radius. Sticky sidebar at ≥900px per Global Constraints. The shell picks its layout with a `useMediaQuery("(min-width: 900px)")` hook. Use semantic markup (`<nav>`, `aria-current="page"` on the active link, a labeled Menu button) so later behavior tests can find elements by role.
- [ ] **Step 4: Check it against the mockup.** Run `npm run dev` and compare with `design/images/01-desktop-home.png`. At 1440px: sidebar links read Home, Plan, Money, Ranks; the current page's link is highlighted; the bell shows no badge with zero unread; the sidebar stays put while the page scrolls. At 390px (browser dev tools): the top bar replaces the sidebar, and Menu opens a list with Home, Plan, Money, Ranks, Ask.
- [ ] **Step 5: Add the `frontend` job to `ci.yml`:** `actions/setup-node` with Node 22, then `npm ci && npm run format:check && npm run lint && npx tsc --noEmit && npx vitest run --passWithNoTests` in `frontend/` (the first frontend tests arrive in Task 6).
- [ ] **Step 6: Git hooks: Conventional Commits and pre-commit formatting.** At the repo root (not in `frontend/`), add a `package.json` with dev dependencies `@commitlint/cli`, `@commitlint/config-conventional`, `husky`, and `lint-staged`; a `pre-commit` hook running `npx lint-staged`, with lint-staged running `prettier --write` and `eslint --fix` on staged `frontend/**/*.{ts,tsx,js,css,json,md}` files and `./backend/mvnw -f backend/pom.xml spotless:apply` when `backend/**/*.java` files are staged; a `commitlint.config.mjs` that extends `@commitlint/config-conventional`; and a Husky `commit-msg` hook running `npx --no -- commitlint --edit "$1"`. Add a `commitlint` job to `ci.yml` that runs on pull requests and lints every commit from the base to the head (`npx commitlint --from ${{ github.event.pull_request.base.sha }} --to ${{ github.event.pull_request.head.sha }}`; checkout with `fetch-depth: 0`). Verify: `git commit --allow-empty -m "bad message"` is rejected, and `git commit --allow-empty -m "chore: test commitlint"` succeeds (then `git reset HEAD~1`).
- [ ] **Step 6a: Dependabot.** Now that both `backend/pom.xml` and `frontend/package.json` exist, add `.github/dependabot.yml` with three ecosystems (`maven` in `/backend`, `npm` in `/frontend` and `/`, `github-actions` in `/`), each checked weekly, with one group per ecosystem (`patterns: ["*"]`) so updates arrive as one PR per ecosystem instead of one per library. Security updates still arrive right away. Commit messages use the `build(deps)` prefix so commitlint accepts them.
- [ ] **Step 6b: Releases.** Add release-please (`googleapis/release-please-action`) on pushes to `main`, with `release-type: simple`, starting at `0.1.0`, `bump-minor-pre-major: true`. It reads the Conventional Commits, keeps a release PR open with the next version and `CHANGELOG.md`, and tags a GitHub release when you merge it. v1 will be a `0.x` version; 1.0.0 is the public release. Publishing a Docker image on each release comes in Milestone 9.
- [ ] **Step 7: Commit.** `git commit -am "feat(web): app shell with sidebar, top bar, page stubs"` (and `build(ci): enforce conventional commits` for Step 6)

### Task 6: Passkey login screen, API client, auth gate

**Files:**
- Create: `frontend/src/lib/api-client.ts`, `frontend/src/features/auth/passkey.ts`, `features/auth/LoginPage.tsx`, `features/auth/RequireAuth.tsx`, `features/auth/use-me.ts`
- Modify: `frontend/src/app/App.tsx` (wrap shell routes in `<RequireAuth>`)
- Test: `frontend/src/lib/api-client.test.ts`, `frontend/src/features/auth/LoginPage.test.tsx`

**Interfaces:**
- Consumes: Spring Security WebAuthn endpoints and `POST /api/v1/setup` (Task 3); `GET /api/v1/me` (Task 4).
- Produces:
  - `apiFetch<T>(path: string, init?: RequestInit): Promise<T>` — sends `credentials: "same-origin"`, adds `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie on non-GET requests, throws `UnauthorizedError` on 401 and `ApiError(status, body)` on other non-2xx.
  - `registerPasskey(): Promise<void>` and `loginWithPasskey(): Promise<void>` in `features/auth/passkey.ts`. Each fetches options JSON, calls `navigator.credentials.create/get` with `PublicKeyCredential.parseCreationOptionsFromJSON` / `parseRequestOptionsFromJSON`, and posts `credential.toJSON()` in the body shapes shown in Spring Security's passkeys reference. Both throw `PasskeyCancelledError` when the browser raises `NotAllowedError`, and `PasskeyUnsupportedError` when `window.PublicKeyCredential` is missing.
  - `useMe()` — a TanStack Query hook over `GET /api/v1/me`, key `["me"]`.
  - `<RequireAuth>` — renders children when `useMe` succeeds, and navigates to `/login` on `UnauthorizedError`. A global `QueryCache` `onError` does the same for any query, so session expiry mid-use also redirects.
  - `<LoginPage>` — "Ranked Finance" title and a "Continue with passkey" button. A secondary "First-time setup" link opens a step that asks for the setup code, posts it to `/api/v1/setup`, then calls `registerPasskey`. Messages: "That setup code isn't right." on `invalid_setup_code`, "This app is already set up. Sign in with your passkey." on `setup_closed`, and "Setup isn't configured on this server. Set SETUP_CODE and restart." on `setup_not_configured`.

- [ ] **Step 1: Write the failing tests.**
  - `api-client.test.ts › addsCsrfHeaderOnPost`: with `document.cookie = "XSRF-TOKEN=abc"`, a POST through a mocked `fetch` carries `X-XSRF-TOKEN: abc`.
  - `api-client.test.ts › throwsUnauthorizedOn401`.
  - `LoginPage.test.tsx › redirectsToLoginOn401`: render the app at `/` with `fetch` returning 401 for `/api/v1/me`; expect the "Continue with passkey" button.
  - `LoginPage.test.tsx › showsMessageWhenPasskeyCancelled`: stub `navigator.credentials.get` to reject with `DOMException("", "NotAllowedError")`; clicking sign-in shows "Sign-in was cancelled. Try again."
  - `LoginPage.test.tsx › showsSetupClosedMessage`: `/api/v1/setup` returns 403 `{"error":"setup_closed"}` → "This app is already set up. Sign in with your passkey."
- [ ] **Step 2: Run them to make sure they fail.** Run: `npx vitest run src/lib src/features/auth`. Expected: FAIL.
- [ ] **Step 3: Implement** `lib/api-client.ts`, `features/auth/passkey.ts`, `features/auth/use-me.ts`, `features/auth/RequireAuth.tsx`, `features/auth/LoginPage.tsx`. Copy for the unsupported case: "This browser doesn't support passkeys. Try the latest Chrome, Safari, or Edge."
- [ ] **Step 4: Run the tests to make sure they pass.** Expected: PASS (5 tests).
- [ ] **Step 5: End-to-end manual check.** Run `docker compose up -d`, start the backend with the `dev` profile, then `npm run dev`. Open `http://localhost:5173`: you're redirected to `/login`. "First-time setup" with the dev setup code (`dev-setup-code-not-secret`) registers a passkey, "Continue with passkey" logs you in, and the Home stub appears inside the shell. Sessions are in memory in M1, so restarting the backend logs you out. That's expected; Spring Session JDBC comes in M7.
- [ ] **Step 6: Commit.** `git commit -am "feat(web): passkey login and auth gate"`

---

**Milestone 1 is done when:** CI is green on all jobs (backend, frontend, commitlint); on a fresh database you can register one passkey and sign in; a second unauthenticated registration is refused; and the shell matches the desktop mockup's sidebar and top bar (`design/images/01-desktop-home.png`).

**Next:** write the Milestone 2 (Plaid sync behind the provider interface) plan with superpowers:writing-plans, starting from the roadmap row, ADR 0007, and the design doc's Constraints, Sync, and Data sources sections.

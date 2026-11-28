# Milestone 2a: Review follow-ups, normalized tables, manual accounts — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Controllers receive the signed-in user's id, every API failure has one documented shape, the app runs as a least-privilege database user, provider tokens can be encrypted, and the owner can add, update, hide, close, and reopen manual accounts on the Settings & accounts screen. Ends with a time-boxed Plaid Sandbox spike whose notes are M2b's input.

**Architecture:** Milestone 2 is split in two (decided Nov 28, 2026). M2a builds everything that doesn't depend on how Plaid behaves: the three architecture-review tasks (#20–#22), the `connections`, `accounts`, and `balance_snapshots` tables, `TokenCipher`, the database roles, and manual accounts, which write straight to the normalized tables (ADR 0007). M2b (Plaid Link, the `DataProvider` interface with Plaid as its first implementation, webhooks, `sync_jobs`, `transactions`, liabilities, reconnect, and the linked-replaces-manual merge) is planned after the spike in Task 10.

**Tech Stack:** as in M1, plus springdoc's `OpenApiCustomizer`, the JDK's AES-GCM (`javax.crypto`, no new dependency), Testcontainers init scripts, and `cloudflared` for the spike only.

**Testing:** follow the Testing section of `CLAUDE.md`: test first for money, auth and security, token encryption, duplicate-account matching, and time-zone date boundaries; no tests for layout, stubs, or plumbing. Every SQL statement runs against real Postgres in at least one test.

**Spec:** `docs/design.md`: Constraints, Sync, Data sources, Data model (connections, accounts, balance_snapshots), Security and hosting. `docs/adr/0007-swappable-data-providers.md`. Issues #20, #21, #22. The grill-me decisions of Nov 28, 2026, recorded in design.md and in this plan.

## Global Constraints

- Every new table carries `user_id uuid not null references users(id)`, and every query takes the user id and filters on it.
- Money is `BigDecimal` / `numeric(14,2)`; APR is `numeric(6,3)`. Money crosses the API as a decimal string (`"8950.00"`), never a JSON number (ADR 0017, Task 8).
- Debts are stored as positive balances; the UI shows them with a minus sign.
- "Today" is computed in the user's time zone (`users.time_zone`) from an injected `Clock`. Never call `now()` without a clock.
- Error responses are RFC 9457 problem details with an `error` code from `ErrorCode`. Codes are only ever added, never renamed or removed (ADR 0014).
- Manual accounts in M2a use the home currency only (`users.home_currency`). The column exists; the picker comes with exchange rates in M8.
- Hidden is display-only: a hidden account still counts toward net worth and ranks everywhere.
- No Plaid code exists in M2a; `connections.provider` exists so nothing assumes Plaid.
- New environment variables: `DB_MIGRATION_USER`, `DB_MIGRATION_PASSWORD`, `ENCRYPTION_KEY`, `ENCRYPTION_KEY_VERSION`. `DB_USER` / `DB_PASSWORD` now mean the restricted app user.

## Review Focus

1. **A setup-only session lands on a page that calls the API.** It must go to the setup screen's passkey step, never loop between `/login` and `/`, and a real `forbidden` must never redirect. Pinned in Task 3: `setupSessionGoesToThePasskeyStep`, `forbiddenNeverRedirects`.
2. **The owner adds a backup passkey after the principal change.** Spring finds the WebAuthn user entity by `Authentication.getName()`, so changing the principal could silently create a second user entity. Pinned in Task 1: `ownerCanStillRequestPasskeyOptions`.
3. **An unexpected exception whose message contains a token.** The response must say `internal_error` with a fixed detail and nothing from the exception. Pinned in Task 2: `unexpectedExceptionNeverLeaksItsMessage`.
4. **The app's database user tries to change the schema or the migration history** (a SQL injection that gets through, or a bug). It must fail. Pinned in Task 4: `appUserCannotChangeTheSchema`, `appUserCannotEditMigrationHistory`.
5. **The owner enters a balance late in the evening on the West Coast.** It must be dated that evening in their zone, not the next UTC day. Pinned in Task 8: `balanceIsDatedInTheUsersTimeZone`.

---

## File Structure

New and changed paths only. Everything else is as in M1.

```
db/init/01-app-role.sql             creates the restricted app role (local compose and Testcontainers)
compose.yaml                        mounts db/init; ignored by Boot's service connection
docs/adr/0014-api-error-codes.md  0015-database-roles.md  0016-token-encryption.md  0017-money-as-decimal-strings.md
docs/spikes/                        Task 10's notes

backend/src/main/java/com/rankedfinance/
  errors/                           new module: owns every failure exit (#21)
    ErrorCode.java  ApiException.java  ApiProblems.java (moved from common/)
    ApiExceptionHandler.java  ProblemErrorController.java  ProblemSecurityHandlers.java
    ErrorCodesOpenApi.java
  user/OwnerPrincipal.java  CurrentUser.java  CurrentUserArgumentResolver.java  CurrentUserWebConfig.java
  auth/OwnerSignIn.java
  sync/TokenCipher.java  AesGcmTokenCipher.java  EncryptionProperties.java
  accounts/                         new module
    AccountKind.java  AccountSource.java  UpdateReminder.java  Account.java
    AccountRepository.java  DuplicateAccountMatcher.java  UpdateReminders.java  AccountHealth.java
    AccountService.java  AccountController.java  (request/response records)
  config/ClockConfig.java
backend/src/main/resources/db/migration/V4__app_role_grants.sql  V5__accounts.sql

frontend/src/
  lib/errors.ts  lib/money.ts
  features/auth/session.ts          new: owns the me query, cache clearing, and signed-out redirects (#22)
  features/accounts/                new feature
  app/pages/SettingsAccountsPage.tsx composes features/accounts (and features/settings in M7)
```

---

### Task 1: The signed-in user's id reaches controllers (#20)

Spring's passkey login makes the principal (Spring Security's "who is signed in" object, like `req.user` in Passport) the WebAuthn user entity, whose name is the owner's email. Spring's passkey *registration* also finds the user entity by `Authentication.getName()`. So the new principal keeps the email as its name and adds the id; controllers only ever see `CurrentUser(id)`.

**Files:**
- Create: `user/OwnerPrincipal.java`, `user/CurrentUser.java`, `user/CurrentUserArgumentResolver.java`, `user/CurrentUserWebConfig.java`, `auth/OwnerSignIn.java`
- Modify: `config/SecurityConfig.java`, `user/MeController.java`
- Test: `backend/src/test/java/com/rankedfinance/TestOwner.java` (shared fixture), `auth/OwnerSignInTest.java`, `auth/OwnerUserDetailsServiceTest.java`, `user/CurrentUserTest.java`, `config/SecurityTest.java` (one new test); update `user/MeControllerTest.java` to use `TestOwner`

**Interfaces:**
- Consumes: `UserRepository.findByEmail`, `findById`, `createOwner` (M1).
- Produces:
  - `record OwnerPrincipal(UUID id, String email) implements java.security.Principal, java.io.Serializable`; `getName()` returns `email`. Serializable because the session stores it.
  - `record CurrentUser(UUID id)`: a controller parameter, filled by `CurrentUserArgumentResolver` (a Spring MVC hook that builds a controller argument, like Express middleware setting `req.user`) from an `OwnerPrincipal`. Any other principal throws `AccessDeniedException` (mapped to `setup_incomplete` or `forbidden` in Task 2).
  - `OwnerSignIn.signedIn(String email): Authentication`: an authenticated token with `OwnerPrincipal(user.id(), email)` and `ROLE_OWNER`; throws `BadCredentialsException` for an unknown email.
  - `TestOwner.create(UserRepository): UUID` and `TestOwner.asOwner(UUID id, String email): RequestPostProcessor`. Every later test signs in through this.

- [ ] **Step 1: Write the failing tests.**
  - `OwnerSignInTest.mapsTheOwnersEmailToTheirId` (`@JdbcTest` with `@AutoConfigureTestDatabase(replace = NONE)`, importing `UserRepository` and `OwnerSignIn`): the principal is an `OwnerPrincipal` with the row's id, the name is the email, and the only authority is `ROLE_OWNER`.
  - `OwnerSignInTest.unknownEmailIsRefused`: throws `BadCredentialsException`.
  - `OwnerUserDetailsServiceTest.ownerGetsTheOwnerRole`.
  - `CurrentUserTest.controllerReceivesTheOwnersId`: `GET /api/v1/me` with `asOwner(id, email)` → `$.id` equals `id`.
  - `CurrentUserTest.setupSessionNeverReachesAController`: a `SETUP` session → `403`.
  - `SecurityTest.ownerCanStillRequestPasskeyOptions`: as `asOwner`, `POST /webauthn/register/options` → `200`, and `user_entities` has exactly one row, named the owner's email.
- [ ] **Step 2: Run them to make sure they fail.** Run: `./mvnw test -Dtest='OwnerSignInTest,OwnerUserDetailsServiceTest,CurrentUserTest,SecurityTest'`. Expected: compilation failure (no `OwnerPrincipal`).
- [ ] **Step 3: Implement the records, the resolver, and `OwnerSignIn`.** Register the resolver in `CurrentUserWebConfig implements WebMvcConfigurer`. `MeController.me(CurrentUser current)` loads the row with `findById`.
- [ ] **Step 4: Make passkey login produce an `OwnerPrincipal`.** Wrap what the `webAuthn()` DSL builds so a successful passkey authentication is replaced by `ownerSignIn.signedIn(authentication.getName())`, using `http.withObjectPostProcessor(...)` on the object the configurer post-processes (check `WebAuthnConfigurer`'s source for which one; Spring calls `postProcess` on objects it builds so apps can decorate them). **If no supported hook exists, stop and report back.** The fallback is the resolver loading the id by email (`users.findByEmail(principal.getName())`), with `OwnerSignIn` deleted.
- [ ] **Step 5: Run the full suite.** Run: `./mvnw spotless:apply && ./mvnw verify`. Expected: all pass. Then sign in with a passkey on the running app (`dev` profile) and check `GET /api/v1/me` returns your id. A real passkey can't run in MockMvc, so this manual check covers Step 4's wiring.
- [ ] **Step 6: Commit.** `git commit -am "feat(user): pass the signed-in user's id to controllers"`

### Task 2: One shape for every failure, codes in the OpenAPI contract (#21)

**Files:**
- Create: `errors/ErrorCode.java`, `errors/ApiException.java`, `errors/ApiExceptionHandler.java`, `errors/ProblemErrorController.java`, `errors/ProblemSecurityHandlers.java`, `errors/ErrorCodesOpenApi.java`, `docs/adr/0014-api-error-codes.md`, `frontend/src/lib/errors.ts`
- Move: `git mv backend/src/main/java/com/rankedfinance/common/ApiProblems.java backend/src/main/java/com/rankedfinance/errors/ApiProblems.java` (then `common/` is empty; leave the package for later shared types)
- Modify: `config/SecurityConfig.java`, every `ApiProblems` caller in `auth/`, `user/MeController.java` (throw `ApiException(NOT_FOUND)`), `CLAUDE.md` (add `errors` to the package list, as the one place failures are shaped), `frontend/src/lib/api-client.ts` (`code` typed `string | undefined` still, for unknown codes), `features/auth/LoginPage.tsx` (its message map moves to `lib/errors.ts`), regenerate `frontend/src/lib/api-schema.d.ts` with `npm run gen:api`
- Test: `errors/ApiExceptionHandlerTest.java`, `errors/ProblemErrorControllerTest.java`, `errors/ErrorCodeContractTest.java`; update `config/SecurityTest.java`, `config/ErrorResponsesTest.java`; `frontend/src/lib/errors.test.ts`

**Interfaces:**
- Consumes: `CurrentUser`'s `AccessDeniedException` (Task 1).
- Produces:
  - `enum ErrorCode`, each with `HttpStatus status()` and `String code()` (`@JsonValue`): `UNAUTHENTICATED(401, "unauthenticated")`, `SETUP_INCOMPLETE(403, "setup_incomplete")`, `FORBIDDEN(403, "forbidden")`, `SETUP_CLOSED(403, "setup_closed")`, `SETUP_NOT_CONFIGURED(403, "setup_not_configured")`, `INVALID_SETUP_CODE(401, "invalid_setup_code")`, `NOT_FOUND(404, "not_found")`, `VALIDATION_FAILED(400, "validation_failed")`, `CONFLICT(409, "conflict")`, `METHOD_NOT_ALLOWED(405, "method_not_allowed")`, `INTERNAL_ERROR(500, "internal_error")`.
  - `ApiException(ErrorCode code, String detail) extends RuntimeException`. Controllers and services throw this; nothing builds a problem by hand.
  - `ApiProblems.of(ErrorCode, String detail): ProblemDetail` and `ApiProblems.write(HttpServletResponse, ErrorCode, String detail, JsonMapper)`. The string-code overloads are deleted.
  - `validation_failed` adds `errors: [{ "field": "...", "message": "..." }]`.
  - Every exit maps to a code: the 401 entry point → `unauthenticated`; access denied → `setup_incomplete` when the authentication has `ROLE_SETUP`, otherwise `forbidden`, and CSRF failures (`CsrfException`) are always `forbidden`; `/error` → by status (400 `validation_failed`, 401 `unauthenticated`, 403 `forbidden`, 404 `not_found`, 405 `method_not_allowed`, anything else `internal_error`), never Boot's default body; `ResponseStatusException` → by status the same way; any other exception → `internal_error` with the fixed detail `"Something went wrong."`, logged server-side.
  - OpenAPI: one `ApiProblem` schema (`type`, `title`, `status`, `detail`, `error` as an enum of every code, optional `errors`), attached as the `4XX` and `5XX` response of every operation. `lib/errors.ts` exports `type ErrorCode = components["schemas"]["ApiProblem"]["error"]`, `errorMessages: Record<ErrorCode, string>`, and `messageFor(error: unknown): string`.

- [ ] **Step 1: Write the failing tests.**
  - `SecurityTest.apiRequiresAuth` (updated): `401`, `Content-Type: application/problem+json`, `$.error == "unauthenticated"`.
  - `SecurityTest.setupSessionGetsSetupIncomplete`: a `SETUP` session, `GET /api/v1/me` → `403`, `$.error == "setup_incomplete"`.
  - `SecurityTest.setupSessionWithoutCsrfIsForbidden`: a `SETUP` session, `POST /logout` without CSRF → `$.error == "forbidden"`.
  - `ErrorResponsesTest.anonymousPostWithoutCsrfIsForbiddenNotUnauthorized` (updated): also asserts the body's `error` is `"forbidden"`.
  - `ProblemErrorControllerTest.errorPathAnswersWithAProblem`: MockMvc `GET /error` with request attribute `jakarta.servlet.error.status_code = 404` → `$.error == "not_found"`, and no `trace`, `exception`, or `message` fields.
  - `ApiExceptionHandlerTest` (`@WebMvcTest` of a test-only controller, `@AutoConfigureMockMvc(addFilters = false)`): `unexpectedExceptionNeverLeaksItsMessage` (it throws `RuntimeException("access-sandbox-token-123")` → `500 internal_error`, and the body doesn't contain `token`); `invalidBodyListsTheFields` (`@Valid` record with `@NotBlank name` → `400 validation_failed`, `$.errors[0].field == "name"`); `wrongMethodIsMethodNotAllowed`; `apiExceptionUsesItsCode` (`ApiException(CONFLICT, "x")` → `409`, `$.error == "conflict"`).
  - `ErrorCodeContractTest.everyCodeIsInTheOpenApiSpec` (`@SpringBootTest(properties = "springdoc.api-docs.enabled=true")`): `components.schemas.ApiProblem.properties.error.enum` equals every `ErrorCode.code()`.
  - `errors.test.ts`: `unknownCodeFallsBackToAGenericMessage`.
- [ ] **Step 2: Run them to make sure they fail.** Run: `./mvnw test -Dtest='SecurityTest,ErrorResponsesTest,ProblemErrorControllerTest,ApiExceptionHandlerTest,ErrorCodeContractTest'`. Expected: compilation failure (no `ErrorCode`).
- [ ] **Step 3: Implement the `errors` module and switch every caller.** `ProblemErrorController` implements Boot's `ErrorController` at `/error`, so it replaces Boot's `BasicErrorController`.
- [ ] **Step 4: Write ADR 0014** (template in `docs/adr/template.md`): the code lives in the `error` extension field, not a `type` URI (no docs URL to host forever; the same pattern as Stripe's and GitHub's error codes); codes are lowercase snake_case; only ever added, never renamed or removed; a new code ships with its frontend message; clients must handle unknown codes.
- [ ] **Step 5: Run the suite.** Run: `./mvnw spotless:apply && ./mvnw verify`. Expected: all pass. Then start the backend (`dev`) and run `npm run gen:api`, and add the messages to `lib/errors.ts`. `npx tsc -b` must pass, and fails if a code has no message.
- [ ] **Step 6: Run the frontend checks.** Run: `npx vitest run && npx tsc -b && npm run lint && npm run format:check`. Expected: all pass.
- [ ] **Step 7: Commit.** `git commit -am "feat: one error shape for every failure, with codes in the openapi contract"`

### Task 3: Frontend session module (#22)

**Files:**
- Create: `frontend/src/features/auth/session.ts`
- Delete: `git rm frontend/src/app/query-client.ts` (moves into `session.ts`)
- Modify: `app/App.tsx`, `features/auth/RequireAuth.tsx`, `features/auth/LoginPage.tsx`, `lib/api-client.ts` (delete `isSignedOut`)
- Test: `app/App.test.tsx`, `features/auth/LoginPage.test.tsx`

**Interfaces:**
- Consumes: `ErrorCode`, `ApiError.code` (Task 2).
- Produces:
  - `createSessionQueryClient(navigate: (to: string) => void): QueryClient`, with a `QueryCache` **and** a `MutationCache` whose `onError` calls `signedOutRedirect`.
  - `signedOutRedirect(error: unknown, currentPath: string): string | null`: `unauthenticated` → `/login?next=<encoded currentPath>`; `setup_incomplete` → `/login?setup=passkey`; anything else, including `forbidden` and a 403 without a code → `null`. On a redirect, the client calls `queryClient.clear()` first, so no data from the old session survives.
  - `safeNext(next: string | null): string`: only a path starting with a single `/` (not `//`, not a URL); otherwise `/`.
  - `LoginPage` reads `?next=` (returns there after sign-in, through `safeNext`) and `?setup=passkey` (opens setup mode at the "create your passkey" step, since the setup code was already accepted).

- [ ] **Step 1: Write the failing tests** (Vitest, mocked `fetch`, problem bodies with `error` codes):
  - `redirectsToLoginWithNextOn401`: rendering `/settings` with `/api/v1/me` → `401 unauthenticated` lands on `/login?next=%2Fsettings`.
  - `redirectsWhenAMutationGets401`: a mutation failing with `unauthenticated` → `/login`.
  - `forgetsCachedDataWhenSignedOut`: after the redirect, the query client has no cached queries.
  - `setupSessionGoesToThePasskeyStep` (replaces `redirectsToLoginForASetupOnlySession`): `403 setup_incomplete` → shows the "Create your passkey" button.
  - `forbiddenNeverRedirects`: `/api/v1/me` → `403 forbidden` shows the alert and stays on `/`.
  - `LoginPage.returnsToNextAfterSignIn` and `LoginPage.ignoresAnOffSiteNext` (`?next=//evil.example` → `/`).
- [ ] **Step 2: Run them to make sure they fail.** Run: `npx vitest run`. Expected: the new tests fail.
- [ ] **Step 3: Implement `session.ts` and wire it in `App.tsx`, `RequireAuth`, and `LoginPage`.**
- [ ] **Step 4: Run the frontend checks.** Run: `npx vitest run && npx tsc -b && npm run lint && npm run format:check`. Expected: all pass.
- [ ] **Step 5: Commit.** `git commit -am "feat(web): one session module for signed-out redirects, keyed on error codes"`

### Task 4: Least-privilege database user

Spring Boot can run Flyway as one database user (`spring.flyway.user`) and the app as another (`spring.datasource.username`), like using separate connection strings for `migrate` and the server in Node. The operator creates both roles; migrations only grant (ADR 0015).

**Files:**
- Create: `db/init/01-app-role.sql`, `backend/src/test/resources/db/02-test-truncate.sql`, `backend/src/main/resources/db/migration/V4__app_role_grants.sql`, `docs/adr/0015-database-roles.md`
- Modify: `compose.yaml` (mount `./db/init:/docker-entrypoint-initdb.d:ro`; label the service `org.springframework.boot.ignore: "true"` so Boot's compose support doesn't replace the app's credentials with the superuser's), `application.yaml`, `TestcontainersConfiguration.java`, `CLAUDE.md` (Commands: one-time `docker compose down -v` note; Security: the role names)
- Test: `ApplicationSmokeTest.java` (three new tests)

**Interfaces:**
- Produces:
  - Roles: `rankedfinance` (owner and migrations; the compose `POSTGRES_USER`) and `rankedfinance_app` (rows only). `01-app-role.sql` is `create role rankedfinance_app login password 'rankedfinance_app';`, a local-dev password that never ships.
  - `application.yaml`: `spring.datasource.username: ${DB_USER:rankedfinance_app}`, `password: ${DB_PASSWORD:rankedfinance_app}`; `spring.flyway.user: ${DB_MIGRATION_USER:rankedfinance}`, `password: ${DB_MIGRATION_PASSWORD:rankedfinance}`; `spring.flyway.placeholders.app_role: ${DB_USER:rankedfinance_app}`.
  - `V4__app_role_grants.sql`:

```sql
grant usage on schema public to ${app_role};
grant select, insert, update, delete on all tables in schema public to ${app_role};
grant usage, select on all sequences in schema public to ${app_role};
alter default privileges in schema public grant select, insert, update, delete on tables to ${app_role};
alter default privileges in schema public grant usage, select on sequences to ${app_role};
revoke all on flyway_schema_history from ${app_role};
```

  - `TestcontainersConfiguration`: the container runs `postgres:19` as user `rankedfinance` with `01-app-role.sql` and `02-test-truncate.sql` copied into `/docker-entrypoint-initdb.d/`. `@ServiceConnection` is replaced by a `DynamicPropertyRegistrar` bean (sets properties from the running container) that points the datasource at `rankedfinance_app` and Flyway at `rankedfinance`. `02-test-truncate.sql` grants `TRUNCATE` on future tables to the app role, so `TestDatabase.deleteAllRows` keeps working. It's test-only.

- [ ] **Step 1: Write the failing tests** in `ApplicationSmokeTest`:
  - `appRunsAsTheRestrictedUser`: `select current_user` → `rankedfinance_app`.
  - `appUserCannotChangeTheSchema`: `create table x (id int)` throws `BadSqlGrammarException` (permission denied).
  - `appUserCannotEditMigrationHistory`: `delete from flyway_schema_history` throws.
- [ ] **Step 2: Run them to make sure they fail.** Run: `./mvnw test -Dtest=ApplicationSmokeTest`. Expected: FAIL, since the app still connects as the container's superuser.
- [ ] **Step 3: Add the init scripts, `V4`, the config, and the Testcontainers change.**
- [ ] **Step 4: Run the full suite.** Run: `./mvnw verify`. Expected: all pass, so every earlier test now runs as the restricted user.
- [ ] **Step 5: Recreate the local database once** (init scripts only run on an empty volume; local data is disposable): `docker compose down -v`, then start the app with the `dev` profile and sign in again through first-run setup.
- [ ] **Step 6: Write ADR 0015**: the operator creates both roles, migrations only grant, no password ever sits in a migration; the hosted and self-hosted setups document the two roles (`SELF_HOSTING.md` in M9).
- [ ] **Step 7: Commit.** `git commit -am "feat(db): run the app as a least-privilege database user"`

### Task 5: Token encryption

**Files:**
- Create: `sync/TokenCipher.java`, `sync/AesGcmTokenCipher.java`, `sync/EncryptionProperties.java`, `docs/adr/0016-token-encryption.md`
- Modify: `application.yaml` (`app.encryption.key: ${ENCRYPTION_KEY:}`, `app.encryption.key-version: ${ENCRYPTION_KEY_VERSION:1}`), `CLAUDE.md` (how to make a key: `openssl rand -base64 32`)
- Test: `sync/AesGcmTokenCipherTest.java`, `sync/EncryptionPropertiesTest.java`

**Interfaces:**
- Produces:
  - `interface TokenCipher { byte[] encrypt(String plaintext); String decrypt(byte[] stored); }`. M2b stores Plaid access tokens with it; M10 adds a KMS implementation behind the same interface.
  - Stored format: `[key version: 1 byte][nonce: 12 random bytes][ciphertext + 16-byte GCM tag]`, AES-256-GCM.
  - `record EncryptionProperties(String key, int keyVersion)` (`@ConfigurationProperties("app.encryption")`, `@Validated`): the key is blank, or base64 that decodes to exactly 32 bytes, or the app refuses to start; `keyVersion` is 1–255; `toString()` never includes the key. A blank key is allowed in M2a (nothing encrypts yet); `encrypt` and `decrypt` then throw `IllegalStateException("ENCRYPTION_KEY is not set")`. M2b makes the key required when Plaid is configured.

- [ ] **Step 1: Write the failing tests.**
  - `AesGcmTokenCipherTest` (plain JUnit): `roundTrips`; `sameTokenEncryptsDifferentlyEachTime`; `tamperedValueIsRejected` (flip one byte → throws); `wrongKeyIsRejected`; `unknownKeyVersionIsRejected`; `refusesToWorkWithoutAKey`.
  - `EncryptionPropertiesTest` (`ApplicationContextRunner`, Spring's lightweight way to start just a few beans): `shortKeyStopsStartup` (a 16-byte key fails); `propertiesNeverPrintTheKey`.
- [ ] **Step 2: Run them to make sure they fail.** Run: `./mvnw test -Dtest='AesGcmTokenCipherTest,EncryptionPropertiesTest'`. Expected: compilation failure.
- [ ] **Step 3: Implement** with `Cipher.getInstance("AES/GCM/NoPadding")` and a `SecureRandom` nonce.
- [ ] **Step 4: Run them to make sure they pass.** Same command. Expected: PASS.
- [ ] **Step 5: Write ADR 0016**: AES-256-GCM from the JDK, the key from `ENCRYPTION_KEY`, a key version on every stored value so a key can be rotated later; envelope encryption and KMS wait for M10; losing the key means re-linking every account, so it's backed up with the database.
- [ ] **Step 6: Commit.** `git commit -am "feat(sync): encrypt provider tokens with aes-gcm"`

### Task 6: Normalized tables and the account repository

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__accounts.sql`, `accounts/AccountKind.java`, `accounts/AccountSource.java`, `accounts/UpdateReminder.java`, `accounts/Account.java`, `accounts/AccountRepository.java`
- Test: `accounts/AccountRepositoryTest.java`

**Interfaces:**
- Produces `V5__accounts.sql`:

```sql
create table connections (
  id                      uuid primary key,
  user_id                 uuid not null references users(id),
  provider                text not null check (provider in ('plaid','simplefin')),
  institution_name        text,
  encrypted_credential    bytea not null,       -- TokenCipher output; never logged or returned
  status                  text not null default 'active' check (status in ('active','needs_reconnect','removed')),
  last_successful_update  timestamptz,
  created_at              timestamptz not null default now()
);
create index connections_user on connections (user_id);

create table accounts (
  id                uuid primary key,
  user_id           uuid not null references users(id),
  connection_id     uuid references connections(id),
  source            text not null check (source in ('provider','import','manual')),
  name              text not null,
  institution_name  text,
  last4             text check (last4 ~ '^[0-9]{4}$'),
  kind              text not null check (kind in ('cash','investment','retirement','hsa','crypto','debt')),
  currency          text not null check (currency in ('USD','CAD')),
  apr               numeric(6,3) check (apr between 0 and 100),
  minimum_payment   numeric(14,2) check (minimum_payment >= 0),
  update_reminder   text not null default 'monthly' check (update_reminder in ('two_weeks','monthly','quarterly','never')),
  hidden            boolean not null default false,
  closed_on         date,
  created_at        timestamptz not null default now(),
  check (source <> 'provider' or connection_id is not null),
  check (kind <> 'debt' or source <> 'manual' or apr is not null)
);
create index accounts_user on accounts (user_id);
create index accounts_connection on accounts (connection_id);

create table balance_snapshots (
  account_id  uuid not null references accounts(id) on delete cascade,
  user_id     uuid not null references users(id),
  date        date not null,
  balance     numeric(14,2) not null,
  primary key (account_id, date)
);
create index balance_snapshots_user_date on balance_snapshots (user_id, date);
```

  Status is derived, not stored: closed when `closed_on` is set; needing an update is computed (Task 7). Connection rows are never deleted, only marked `removed` (M2b counts them for `PLAID_ITEM_LIMIT`).
- Produces the Java side:
  - Enums `AccountKind { CASH, INVESTMENT, RETIREMENT, HSA, CRYPTO, DEBT }`, `AccountSource { PROVIDER, IMPORT, MANUAL }`, `UpdateReminder { TWO_WEEKS, MONTHLY, QUARTERLY, NEVER }`, stored as their lowercase names.
  - `record Account(UUID id, AccountSource source, String name, String institutionName, String last4, AccountKind kind, String currency, BigDecimal apr, BigDecimal minimumPayment, UpdateReminder updateReminder, boolean hidden, LocalDate closedOn, BigDecimal balance, LocalDate balanceAsOf, int snapshotCount)`. `balance` and `balanceAsOf` come from the latest snapshot.
  - `record NewManualAccount(String name, String institutionName, AccountKind kind, String currency, BigDecimal apr, BigDecimal minimumPayment, UpdateReminder updateReminder)`.
  - `AccountRepository` (`JdbcClient`), every method taking `UUID userId` first: `List<Account> findAll(userId)` (the latest snapshot per account via `left join lateral (... order by date desc limit 1)`, plus a snapshot count); `Optional<Account> findById(userId, id)`; `UUID insertManual(userId, NewManualAccount)`; `void upsertSnapshot(userId, accountId, LocalDate date, BigDecimal balance)` (`on conflict (account_id, date) do update`); `void update(userId, id, String name, String institutionName, BigDecimal apr, BigDecimal minimumPayment, UpdateReminder reminder, boolean hidden)`; `void setClosedOn(userId, id, LocalDate closedOnOrNull)`; `void delete(userId, id)`.

- [ ] **Step 1: Write the failing tests** (`@JdbcTest`, `replace = NONE`, importing `AccountRepository` and `UserRepository`, an owner created per test):
  - `listShowsEachAccountsLatestBalance`: snapshots on Nov 1 (`100.00`) and Nov 20 (`250.50`) → `balance == 250.50`, `balanceAsOf == 2026-11-20`, `snapshotCount == 2`.
  - `sameDaySnapshotOverwrites`: two upserts on one date → one row, the second balance.
  - `accountWithNoSnapshotsHasNoBalance`: `balance` and `balanceAsOf` are null.
  - `manualDebtWithoutAnAprIsRejected`: the insert throws `DataIntegrityViolationException`.
  - `deletingAnAccountDeletesItsSnapshots`.
- [ ] **Step 2: Run them to make sure they fail.** Run: `./mvnw test -Dtest=AccountRepositoryTest`. Expected: compilation failure.
- [ ] **Step 3: Write `V5` and implement the repository.**
- [ ] **Step 4: Run the full suite.** Run: `./mvnw verify`. Expected: all pass, including `ModularityTest`.
- [ ] **Step 5: Commit.** `git commit -am "feat(accounts): add connections, accounts, and balance snapshots tables"`

### Task 7: Account rules: duplicates, update reminders, health

Pure classes: data and the date in, a result out. No database, no Spring.

**Files:**
- Create: `accounts/DuplicateAccountMatcher.java`, `accounts/UpdateReminders.java`, `accounts/AccountHealth.java`
- Test: `accounts/DuplicateAccountMatcherTest.java`, `accounts/UpdateRemindersTest.java`, `accounts/AccountHealthTest.java`

**Interfaces:**
- Consumes: `Account`, the enums (Task 6).
- Produces:
  - `DuplicateAccountMatcher.matches(Account candidate, List<Account> existing): List<Account>`. Same kind is required; closed accounts never match. If both have an institution and `last4`, they match when the normalized institutions and the `last4` are equal (M2b's linked rule). Otherwise, if both are manual, they match when the normalized names are equal. Normalizing strips, collapses inner whitespace, and lowercases with `Locale.ROOT`. `static String normalize(String)` is public, and the screen groups institutions with the same rule.
  - `UpdateReminders.needsUpdate(Account account, LocalDate today): boolean`: false for closed, hidden, `NEVER`, and non-manual accounts (M2b flags broken connections separately); true when there's no snapshot, or when `today` is after `balanceAsOf` plus the period (`TWO_WEEKS` = 14 days, `MONTHLY` = 1 month, `QUARTERLY` = 3 months, using `LocalDate.plusMonths`).
  - `AccountHealth.summarize(List<Account> accounts, LocalDate today): AccountHealth` with `int upToDate`, `int needsAttention`. Closed accounts count in neither; hidden accounts count as up to date.

- [ ] **Step 1: Write the failing tests** (plain JUnit, with small builders for `Account` so each test sets only what it needs):
  - Matcher: `sameNameAndKindMatches`; `nameIgnoresCaseAndSpaces` (`" Fidelity  Roth "` vs `"fidelity roth"`); `differentKindNeverMatches`; `closedAccountsNeverMatch`; `institutionAndLast4Match`; `sameLast4AtAnotherInstitutionDoesNotMatch`.
  - Reminders: `twoWeeksIsDueOnDay15` (Nov 1 → not due Nov 15, due Nov 16); `monthlyIsDueTheDayAfterAMonth` (Oct 31 → not due Nov 30, due Dec 1); `monthlyFromJan31` (due Mar 1, 2027); `quarterlyIsDueAfterThreeMonths`; `hiddenNeverNeedsUpdate`; `closedNeverNeedsUpdate`; `neverMeansNever`; `noBalanceYetNeedsUpdate`.
  - Health: `closedAccountsCountNowhere`; `hiddenAccountsCountAsUpToDate`.
- [ ] **Step 2: Run them to make sure they fail.** Run: `./mvnw test -Dtest='DuplicateAccountMatcherTest,UpdateRemindersTest,AccountHealthTest'`. Expected: compilation failure.
- [ ] **Step 3: Implement the three classes.**
- [ ] **Step 4: Run them to make sure they pass.** Same command. Expected: PASS.
- [ ] **Step 5: Commit.** `git commit -am "feat(accounts): add duplicate matching and update reminder rules"`

### Task 8: Manual accounts API

**Files:**
- Create: `accounts/AccountService.java`, `accounts/AccountController.java`, request and response records next to them, `config/ClockConfig.java`, `docs/adr/0017-money-as-decimal-strings.md`
- Modify: `user/UserRepository.java` (add `ZoneId timeZone(UUID userId)` and `String homeCurrency(UUID userId)`)
- Test: `accounts/AccountControllerTest.java`, `backend/src/test/java/com/rankedfinance/FixedClock.java`

**Interfaces:**
- Consumes: `CurrentUser` (Task 1), `ApiException` and `ErrorCode` (Task 2), `AccountRepository` (Task 6), the rules (Task 7).
- Produces (all under `/api/v1`, signed-in owner only, money as decimal strings):
  - `ClockConfig`: `@Bean Clock clock()` returning `Clock.systemUTC()`. `FixedClock` is a `@TestConfiguration` replacing it with `Clock.fixed(...)`.
  - `GET /accounts` → `200 List<AccountResponse>`. `AccountResponse(UUID id, String source, String name, String institutionName, String kind, String currency, String balance, LocalDate balanceAsOf, String apr, String minimumPayment, String updateReminder, boolean hidden, LocalDate closedOn, boolean needsUpdate, boolean canDelete)`. Order: open accounts by institution then name, closed accounts last.
  - `GET /accounts/health` → `200 AccountHealthResponse(int upToDate, int needsAttention)`.
  - `POST /accounts/matches` with `CreateManualAccountRequest` → `200 List<AccountMatch(UUID id, String name)>`.
  - `POST /accounts/manual` with `CreateManualAccountRequest(@NotBlank String name, String institutionName, @NotNull String kind, @NotNull String balance, String apr, String minimumPayment, String updateReminder)` → `201 AccountResponse`. Creates the account and today's snapshot in one transaction. `validation_failed` when a debt has no APR, the APR is outside 0–100, a balance is negative for any kind except cash, or the amount has more than 2 decimal places. The currency is always the home currency. `updateReminder` defaults to `monthly`.
  - `PUT /accounts/{id}/balance` with `BalanceRequest(@NotNull String balance)` → `200 AccountResponse`: upserts today's snapshot. `conflict` when the account is closed or isn't manual.
  - `PATCH /accounts/{id}` with `UpdateAccountRequest(String name, String institutionName, String apr, String minimumPayment, String updateReminder, Boolean hidden)` → `200`. Absent fields are unchanged; the same validation as create.
  - `POST /accounts/{id}/close` with `CloseAccountRequest(@NotNull LocalDate closedOn)` → `200`. `validation_failed` if `closedOn` is after today. `POST /accounts/{id}/reopen` → `200`.
  - `DELETE /accounts/{id}` → `204`. `conflict` when the account has more than one snapshot (`canDelete` is `snapshotCount <= 1`).
  - Any id not in the user's accounts → `not_found`.
  - "Today" is `LocalDate.now(clock.withZone(users.timeZone(userId)))`.

- [ ] **Step 1: Write the failing tests** (`@SpringBootTest @AutoConfigureMockMvc`, `TestOwner`, `FixedClock` at `2026-11-28T04:30:00Z`, owner time zone set to `America/Los_Angeles`):
  - `balanceIsDatedInTheUsersTimeZone`: create → `$.balanceAsOf == "2026-11-27"`.
  - `secondBalanceTheSameDayReplacesTheFirst`: `PUT` balance twice → one snapshot, the second value.
  - `moneyIsSentAsDecimalStrings`: balance `"8950"` → `$.balance == "8950.00"` (a string).
  - `manualDebtRequiresAnApr`: → `400 validation_failed`, `$.errors[0].field == "apr"`.
  - `negativeBalanceIsOnlyAllowedForCash`: an investment at `"-5.00"` → `400`; cash at `"-5.00"` → `201`.
  - `fractionsOfACentAreRejected`: `"8.999"` → `400`.
  - `matchesFindsAManualAccountWithTheSameName`.
  - `closeCannotBeInTheFuture`: `closedOn = 2026-11-28` (tomorrow in Los Angeles) → `400`.
  - `closedAccountsCannotBeUpdated`: → `409 conflict`.
  - `deleteIsRefusedOnceThereIsHistory`: two snapshots on different days → `409`.
  - `unknownAccountIsNotFound`: a random id → `404 not_found`.
- [ ] **Step 2: Run them to make sure they fail.** Run: `./mvnw test -Dtest=AccountControllerTest`. Expected: compilation failure.
- [ ] **Step 3: Implement the service, the controller, and `ClockConfig`.** Parse money with `new BigDecimal(text)` and reject `scale() > 2`; write it with `toPlainString()` after `setScale(2)`.
- [ ] **Step 4: Write ADR 0017**: money crosses the API as decimal strings, because a JSON number becomes a JavaScript float; the same rule for every future money field; the frontend formats with `Intl.NumberFormat` and parses input with `lib/money.ts`.
- [ ] **Step 5: Run the full suite.** Run: `./mvnw spotless:apply && ./mvnw verify`. Expected: all pass.
- [ ] **Step 6: Commit.** `git commit -am "feat(accounts): add the manual accounts api"`

### Task 9: Settings & accounts screen

Match `design/images/05-desktop-settings-accounts.png` (desktop) and `23-settings-accounts.png` (phone). M2a builds the header and the left column only.

**Files:**
- Create: `frontend/src/app/pages/SettingsAccountsPage.tsx`, `frontend/src/lib/money.ts`, `frontend/src/features/accounts/` (`api.ts`, `AccountList.tsx`, `AccountCard.tsx`, `ManualBalanceForm.tsx`, `AddManualAccountDialog.tsx`, `AccountMenu.tsx`, `CloseAccountDialog.tsx`, `text.ts`)
- Delete: `git rm frontend/src/features/settings/SettingsPage.tsx` (the route renders the composed page; M7 adds `features/settings` sections to it)
- Modify: `app/router.tsx`, `app/shell/Sidebar.tsx`, `app/shell/PhoneBar.tsx`, `app/shell/nav.ts` (the constant `healthLine` goes), `frontend/.oxlintrc.json` (an override for `src/features/accounts/**`, as for `auth`), regenerate `api-schema.d.ts`
- Test: `frontend/src/lib/money.test.ts`, `frontend/src/features/accounts/AddManualAccountDialog.test.tsx`

**Interfaces:**
- Consumes: the Task 8 endpoints through generated types; `messageFor` (Task 2).
- Produces:
  - `parseMoneyInput(text: string): string | null`: accepts `8950`, `8,950.5`, `$8,950.50`, and a leading `-`; returns a decimal string with 2 places (`"8950.50"`) or `null` for anything else, including more than 2 decimal places. `formatMoney(amount: string, currency: string, { debt }: { debt?: boolean }): string` with `Intl.NumberFormat`; debts show a minus sign.
  - `features/accounts/api.ts`: TanStack Query hooks for every endpoint; mutations invalidate `["accounts"]` and `["accounts", "health"]`.
  - Screen: the header ("Settings & accounts", the subtitle, and **Connect an account**, which opens `AddManualAccountDialog` directly while Manual is the only source). One card per institution (grouped with the backend's normalize rule, mirrored in `api.ts`); an account with no institution gets its own card titled with its name. A manual account shows "Entered by hand · updated {relative time}" and the inline **Balance today / Update** form. A hidden account is dimmed with a "Hidden" badge and an eye toggle. A closed account sorts last, dimmed, with "Closed {date}". An account past its reminder shows a warning badge (warning color plus text, never color alone). The ··· menu has Rename, Edit details, Hide/Unhide, Mark closed (`CloseAccountDialog`, date defaulting to today), Reopen, and Delete (only when `canDelete`). Before creating, the dialog calls `/accounts/matches`; with a match it asks "Is this the same as {name}?": **Yes** updates that account's balance, **No, add it** creates the new one.
  - Sidebar and phone bar: "{n} up to date · {m} need attention" from `/accounts/health` ("No accounts yet" when both are 0); the dot uses the warning color when `m > 0`.

- [ ] **Step 1: Write the failing tests.**
  - `money.test.ts`: `parsesDollarsAndCents` (the inputs above); `rejectsFractionsOfACent`; `rejectsText`.
  - `AddManualAccountDialog.test.tsx` (duplicate-account matching): `asksBeforeAddingADuplicate` (matches return one account → the question appears and nothing is created yet); `sameAccountUpdatesTheExistingBalance` (Yes → `PUT /accounts/{id}/balance`, no `POST /accounts/manual`); `differentAccountIsCreated` (No → `POST /accounts/manual`).
- [ ] **Step 2: Run them to make sure they fail.** Run: `npx vitest run`. Expected: the new tests fail.
- [ ] **Step 3: Build the feature, the page, and the health line.** All UI text in `features/accounts/text.ts`.
- [ ] **Step 4: Run the frontend checks.** Run: `npx vitest run && npx tsc -b && npm run lint && npm run format:check`. Expected: all pass.
- [ ] **Step 5: Check against the mockups** (layout isn't unit-tested). Run the app, add a cash account, a debt, and a Roth IRA under "Fidelity"; update a balance; hide one; close and reopen one; try a duplicate name. Compare with mockup 05 at desktop width and 23 at 390px. Tab through the screen: every control is reachable with visible focus.
- [ ] **Step 6: Commit.** `git commit -am "feat(accounts): add manual accounts to the settings and accounts screen"`

### Task 10: Plaid Sandbox spike (throwaway)

No tests and no merge: CLAUDE.md says to spike an unfamiliar API, then rebuild what you keep test-first. **Time box: 2 days.** Work on `chore/plaid-sandbox-spike` and never open a PR for it; only the notes are merged.

**Files:**
- Create: `docs/spikes/<date>-plaid-sandbox.md` (merged on its own branch, `chore/plaid-sandbox-notes`)

- [ ] **Step 1: Set up.** Sign up for Plaid and take the **Sandbox** keys only. Run `cloudflared tunnel --url http://localhost:8080` and use its URL as the webhook URL. Add the Plaid Java SDK to the spike branch.
- [ ] **Step 2: Answer these, writing each answer and any surprise into the notes:**
  1. Link: the `/link/token/create` settings for transactions plus liabilities (`products`, `required_if_supported_products`, `optional_products`), `days_requested: 730`, and whether investments can be optional. Which Sandbox institutions and test users return liabilities and investments.
  2. Webhooks after Link: which ones arrive, in what order, and with what delay (`SYNC_UPDATES_AVAILABLE`, `INITIAL_UPDATE`, `HISTORICAL_UPDATE`); `/sandbox/item/fire_webhook`.
  3. Verification: verify one real `Plaid-Verification` JWT end to end (ES256, `kid`, `/webhook_verification_key/get`, `iat`, body SHA-256), and pick the JWT library (it must be AGPL-compatible; Nimbus JOSE + JWT is Apache-2.0).
  4. `/transactions/sync`: page sizes, the cursor, `has_more`, how `added`, `modified`, and `removed` look, and how a pending transaction becomes posted (`pending_transaction_id`); the mutation-during-pagination error.
  5. `/accounts/get` and `/liabilities/get`: the type/subtype values and their mapping to `AccountKind`, whether `mask` is always present (the duplicate check needs `last4`), which APR a credit card reports (purchase, cash advance, balance transfer), minimum payments.
  6. Broken logins: `/sandbox/item/reset_login`, the `ITEM_LOGIN_REQUIRED` / `PENDING_DISCONNECT` webhooks, and Link update mode.
  7. Removing an Item, and what Plaid still sends for it.
  8. What the access token, Item id, and account ids look like, so the no-tokens-in-logs test can match them.
- [ ] **Step 3: Draft M2b's `DataProvider` interface** in the notes: the method list from ADR 0007 (connect, list accounts, sync balances and transactions, connection status, reconnect) with the shapes the spike showed.
- [ ] **Step 4: Merge the notes** through a PR from `chore/plaid-sandbox-notes`; delete the spike branch without merging. M2b's plan is written from these notes and the M2b notes in design.md's Decision log.

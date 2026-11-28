# 0014. API error codes

- **Status:** Accepted
- **Date:** 2026-11-28

## Context
Every API error is an RFC 9457 problem detail (CLAUDE.md). The frontend needs to tell failures apart (a wrong setup code, a setup session that hasn't finished, a closed account) without parsing English `detail` text, and the API becomes public in 1.0, so whatever identifies a failure is part of the v1 contract.

RFC 9457's own answer is the `type` member: a URI per kind of problem, ideally pointing at a page that documents it. That means hosting a docs URL for every code forever, for every self-hosted install too. The other common pattern, used by Stripe (`code`) and GitHub (`error` codes in its OAuth and validation responses), is a short string in an extension member, which RFC 9457 allows.

Before this ADR, codes were free strings at each call site, Spring Security answered 401 with an empty body, and Boot's `/error` page answered with its own JSON shape.

## Decision
- The code lives in the `error` extension member of the problem detail, not in a `type` URI. `type` stays unset (read as `about:blank`).
- Codes are lowercase snake_case (`setup_closed`) and are listed in one enum, `errors/ErrorCode`, each with its HTTP status. Code throws `ApiException(ErrorCode, detail)`; nothing builds a problem by hand.
- Every exit uses that shape: controllers, Spring MVC's own exceptions, Spring Security's 401 and 403, and `/error`. Failures with only a status map by status (400 `validation_failed`, 401 `unauthenticated`, 403 `forbidden`, 404 `not_found`, 405 `method_not_allowed`, anything else `internal_error`). An unexpected exception is `internal_error` with a fixed detail; its message is logged, never sent.
- `validation_failed` adds `errors: [{ field, message }]`.
- The OpenAPI spec has one `ApiProblem` schema whose `error` is an enum of every code, attached as the `4XX` and `5XX` response of every operation, so the generated TypeScript types list the codes.
- Codes are only ever added, never renamed or removed. The M1 codes `last_passkey` and `owner_mismatch`, shipped in 0.1.0, are kept for that reason. A new code ships with its message in the frontend's `lib/errors.ts` (the type check fails without one).
- Clients must handle codes they don't know; the frontend falls back to a generic message.

## Consequences
- The frontend switches on a typed code and can't miss a message for a code the backend sends.
- Codes are coarse on purpose: most specific cases share a general code such as `conflict` and are told apart by `detail`. A case the UI must handle differently needs its own new code.
- A code can't be retired, even if nothing sends it any more; it stays in the enum and the frontend map.
- No per-code docs pages; if 1.0's public API needs them, `type` URIs can be added later alongside `error` without breaking clients.

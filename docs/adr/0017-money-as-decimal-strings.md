# 0017. Money crosses the API as decimal strings

- **Status:** Accepted
- **Date:** 2026-11-28

## Context
The API sends and receives money. A JSON number is parsed by JavaScript as a 64-bit float, so a value like `0.1 + 0.2` or a large balance can silently pick up error on the way to the screen. Our rule is that money is `BigDecimal` / `numeric(14,2)` everywhere. Options: JSON numbers, integer cents, or decimal strings.

## Decision
Every money field in the API, request or response, is a decimal string with a dot and exactly 2 decimal places on output (`"8950.00"`). The server parses with `BigDecimal` and rejects more than 2 decimal places. The same rule applies to every future money field. The frontend formats with `Intl.NumberFormat` and parses user input with `lib/money.ts`; it never does float arithmetic on money.

## Consequences
No precision loss between Postgres, Java, and the browser, and the OpenAPI types say `string`. Clients must parse before doing math (use a decimal library or integer cents), and API consumers see strings rather than numbers. Rates such as APR are percentages, not money, and are also sent as strings for consistency.

# 0007. Swappable bank-data providers, plus file import

- **Status:** Accepted
- **Date:** 2026-11-19

## Context
The app has to be easy to self-host. Plaid works for the hosted version, but a self-hoster would need their own Plaid developer account. Self-hosted finance apps in North America mostly use SimpleFIN Bridge instead, which the user pays for directly (about $15 a year) and which needs no developer keys. Aggregators also come and go: GoCardless stopped taking new accounts for its free European bank data in 2025, which broke the setups of many self-hosters. Some people won't link a bank at all and want to import their bank's CSV, OFX, or QIF exports. Actual Budget and Sure both send several providers into one normalized data format.

## Decision
- Every bank-data source implements one `DataProvider` interface: connect, list accounts, sync balances and transactions, report connection status, and reconnect.
- Two implementations to start: Plaid (webhook-driven, in v1) and SimpleFIN (a scheduled daily pull, in 1.0, the first public release).
- File import (CSV, OFX/QFX, QIF) and manual entry write to the same normalized tables.
- Ranks, plan math, and the AI read only the normalized tables (`connections`, `accounts`, `transactions`, `balance_snapshots`), never a provider's own fields.
- Which providers are offered is configuration; a provider that isn't configured is hidden.

## Consequences
- A new aggregator (MX, Teller, an open-banking API) is one new class, with no changes to ranks or the plan.
- Data only some providers supply (APRs, investment contributions) needs a fallback everywhere it's used. The fallbacks already exist for manual accounts: enter the APR once; enter or infer contributions.
- `plaid_items` becomes `connections`, with a `provider` column; accounts and transactions record their source.
- Duplicate detection has to work across sources: provider or OFX transaction IDs where available, otherwise date, amount, and cleaned-up description.
- Tests mock each provider at the interface; Plaid Sandbox and a SimpleFIN demo token are used for exploratory spikes.

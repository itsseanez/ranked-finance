# Ranked Finance

A ranked personal finance app: climb from **Iron to Legend** (financial independence) with a plan built from your real accounts and an AI that knows it.

> **Status:** in design. The build starts January 2027; there's no code to run yet.

## The idea

Most finance apps show you numbers. Ranked Finance turns them into a ladder everyone climbs by the same rules, built on standard financial advice:

| Rank | Milestone |
| --- | --- |
| Iron | Accounts connected |
| Bronze | $1,000 starter emergency fund |
| Silver | No high-interest debt (over 8% APR) |
| Gold | 3 months of your actual spending saved |
| Platinum | Debt-free (besides a mortgage) and 15% of income invested |
| Diamond · Master · Grandmaster | $100k · $250k · $500k net worth |
| Legend | Financial independence: invested assets of 25× your yearly spending |

What makes it different:

- **Ranks can drop.** Keeping wealth is part of the game. A 14-day buffer stops market swings from flipping your rank daily.
- **Your plan is your route up.** Steps are generated from your real accounts (one per card or loan to pay off), locked in ladder order, with your own goals placed between them.
- **The AI explains; the code does the math.** Every number, rank, and date is computed in code. The AI answers questions about your plan but never decides your rank or edits your plan.
- **Net worth first.** It's the first thing on Home, with its full history.
- **Yours to run.** Self-host it with `docker compose up`. Connect banks through Plaid or SimpleFIN, import any bank's CSV, OFX, or QIF files, or enter accounts by hand. Use Claude, OpenAI, or any OpenAI-compatible model, including a local one, or no AI at all.

## Planned stack

Java 25 and Spring Boot · PostgreSQL with Flyway · React, TypeScript, and Vite · Plaid or SimpleFIN for read-only account data, plus file import · Spring AI (Claude by default) · passkey login · Docker for self-hosting

The app only **reads** account data. It never moves money, and it never stores bank logins.

## Docs

- [Design](docs/design.md): what's being built and why, every rank rule, the data model
- [Roadmap](ROADMAP.md): v1 (the first demoable product) and 1.0 (the first public, self-hostable release), milestone by milestone
- [Decisions](docs/adr/): architecture decision records (why Java, why AGPL, and so on)
- [Milestone 1 plan](docs/plans/2026-11-18-m1-foundation.md): the first step-by-step build plan

Setup instructions will be added once Milestone 1 is built.

## Security

Found a vulnerability? Please report it privately. See [SECURITY.md](SECURITY.md). Don't open a public issue.

## Contributing

Not accepting code contributions yet. Issues with ideas and feedback are welcome. Once contributions open, they'll require signing a contributor license agreement (CLA).

## License

[GNU Affero General Public License v3.0](LICENSE). You can use, study, and modify the code; if you run a modified version as a network service, you must publish its source under the same license. The "Ranked Finance" name and logo aren't covered by the license.

# Architecture Decision Records

Short records of hard-to-reverse decisions: what was decided, why, and what it costs. Format follows Michael Nygard's ADR template. Files are named `NNNN-title-with-dashes.md`.

To add one, copy [template.md](template.md) to the next number. Don't rewrite an accepted ADR to change the decision; write a new one that supersedes it and update the old one's status.

| # | Decision | Status |
| --- | --- | --- |
| [0001](0001-java-and-spring-boot-backend.md) | Java and Spring Boot for the backend | Accepted |
| [0002](0002-agpl-3-license.md) | AGPL-3.0 license | Accepted |
| [0003](0003-flyway-for-schema-migrations.md) | Flyway for schema migrations | Accepted |
| [0004](0004-database-access-approach.md) | Database access: `JdbcClient` or Spring Data JPA | Proposed |
| [0005](0005-formatting-and-linting.md) | Formatting and linting (Prettier, ESLint, Spotless) | Accepted |
| [0006](0006-feature-based-structure.md) | Feature-based structure on both sides | Accepted |
| [0007](0007-swappable-data-providers.md) | Swappable bank-data providers, plus file import | Accepted |
| [0008](0008-swappable-ai-providers.md) | Swappable AI providers; the app works without AI | Accepted |

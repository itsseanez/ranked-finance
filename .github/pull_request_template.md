## What and why

<!-- One or two sentences. Link the issue or roadmap milestone. -->

## Checklist

- [ ] Tests written first for anything on CLAUDE.md's test-first list (money, ranks, plan math, auth, matching, imports)
- [ ] New tables have the indexes their queries need
- [ ] Every new query filters by `user_id` and uses named parameters (`:name`), never string concatenation, and runs in at least one test against real Postgres
- [ ] `docs/design.md` updated if a product rule changed (and an ADR added if a hard-to-reverse technical decision was made)

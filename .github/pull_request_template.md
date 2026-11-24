## Why

<!-- The problem this solves and why this approach. Link the issue or roadmap milestone. -->

## What changed

<!-- The main changes at a high level, plus decisions the code doesn't show. -->

## How it was tested

<!-- Tests that failed first and now pass, the commands you ran, and any manual checks. -->

## Where to look

<!-- Anything tricky or risky that a reviewer should read closely. Delete if there's nothing. -->

## Checklist

- [ ] Tests written first for anything on CLAUDE.md's test-first list (money, ranks, plan math, auth, matching, imports)
- [ ] New tables have the indexes their queries need
- [ ] Every new query filters by `user_id` and uses named parameters (`:name`), never string concatenation, and runs in at least one test against real Postgres
- [ ] `docs/design.md` updated if a product rule changed (and an ADR added if a hard-to-reverse technical decision was made)

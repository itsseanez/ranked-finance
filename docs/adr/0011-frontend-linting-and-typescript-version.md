# 0011. Oxlint for frontend linting; TypeScript 7 with TypeScript 6 alongside for tools

- **Status:** Accepted
- **Date:** 2026-11-22
- **Supersedes:** the frontend linting part of [0005](0005-formatting-and-linting.md)

## Context
ADR 0005 chose ESLint with typescript-eslint and `eslint-plugin-import` for the frontend. When the frontend was scaffolded (create-vite 9.3, November 2026), the official `react-ts` template had changed: it ships Oxlint instead of ESLint, and TypeScript 7.0, the native (Go) compiler.

TypeScript 7 doesn't ship the JavaScript compiler API that code generators and lint plugins import; Microsoft expects a new, different API in 7.1. As of November 2026:
- `openapi-typescript`, which generates `src/lib/api-schema.d.ts` from the backend's OpenAPI spec, needs that API (its peer range is TypeScript 5).
- `typescript-eslint` supports TypeScript below 6.1.
- `eslint-plugin-import`, which the plan used for import boundaries, supports ESLint up to 9, not the current ESLint 10.

For the transition, Microsoft publishes `@typescript/typescript6` and recommends npm aliases: `typescript` points to TypeScript 6 (for tools that import it), and TypeScript 7 is installed under another name so `npx tsc` runs 7. Nx, ts-jest and Microsoft's own repos document the same setup.

Options considered:
1. Pin TypeScript 5.9 for everything. Works with no overrides, but ignores both the template and Microsoft's recommended setup.
2. Replace the template's linter with ESLint 9, typescript-eslint and `eslint-plugin-import`. Goes against the template and pins ESLint a major behind.
3. Keep the template's Oxlint; run TypeScript 7 side by side with TypeScript 6.

## Decision
Option 3.
- Linting: Oxlint (the create-vite default) for code quality; Prettier for formatting, unchanged from 0005.
- TypeScript: `"@typescript/native": "npm:typescript@^7"` provides `tsc` (type-checking in CI and `npm run build`); `"typescript": "npm:@typescript/typescript6@^6"` is what tools importing `typescript` get. An npm `overrides` entry lets `openapi-typescript` use TypeScript 6 despite its `^5` peer range. Its output was checked to be identical under 5.9 and 6.0.
- Import boundaries (ADR 0006): Oxlint's `no-restricted-imports`, one override per feature folder in `frontend/.oxlintrc.json`, because Oxlint has no `import/no-restricted-paths`. A new feature folder needs a new override block.

The backend side of 0005 (Spotless, google-java-format) is unchanged.

## Consequences
- Fast type-checking and linting, and a setup that matches new Vite projects and Microsoft's guidance.
- Two TypeScript versions in `package.json` until the tools move to the 7.1 API. When `openapi-typescript` supports TypeScript 7, remove the alias, the override, and `@typescript/typescript6` in one PR.
- No type-aware lint rules for now (Oxlint's type-aware mode is a separate add-on); `tsc` in CI still catches type errors.
- Dependabot PRs that touch the two TypeScript entries need a closer look.

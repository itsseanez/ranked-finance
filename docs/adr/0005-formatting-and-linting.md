# 0005. Formatting and linting

- **Status:** Accepted; frontend linting superseded by [0011](0011-frontend-linting-and-typescript-version.md)
- **Date:** 2026-11-19

## Context
Consistent formatting keeps diffs small and reviews focused on behavior. The frontend (TypeScript) and backend (Java) need separate tools. Prettier's and typescript-eslint's guidance is to use a formatter for formatting and a linter only for code quality; running Prettier inside ESLint (`eslint-plugin-prettier`) is slower and noisier.

## Decision
- Frontend: Prettier for formatting (defaults), ESLint flat config with typescript-eslint for code quality, and `eslint-config-prettier` last to disable formatting rules. No `eslint-plugin-prettier`.
- Backend: Spotless Maven plugin with google-java-format in AOSP style (4-space indent); `spotless:check` runs in `verify`. Chosen over palantir-java-format because the maintainer uses LazyVim and VS Code: conform.nvim (LazyVim's formatter) ships google-java-format built in, and google-java-format has an official VS Code extension, while palantir has only a community VS Code extension and no built-in conform formatter.
- Enforcement: a Husky `pre-commit` hook with lint-staged formats staged files; CI runs `prettier --check`, `eslint`, and `spotless:check`.

## Consequences
- No style debates or formatting-only diffs; editors format on save with the same tools.
- A small amount of tooling at the repo root (Node for hooks) even though the backend is Java.
- Rule changes go through one config per side.

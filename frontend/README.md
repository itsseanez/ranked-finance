# Ranked Finance web app

React + TypeScript + Vite. See the repo root `CLAUDE.md` for commands and conventions.

```sh
npm ci
npm run dev        # http://localhost:5173, proxies /api to the backend on :8080
npx vitest run     # tests
npm run gen:api    # regenerate src/lib/api-schema.d.ts (backend must be running with the dev profile)
npm run format && npm run lint
```

# 0001. Java and Spring Boot for the backend

- **Status:** Accepted
- **Date:** 2026-11-19

## Context
The maintainer knows TypeScript and Node well and had not written Java. TypeScript everywhere would ship v1 sooner and share types between frontend and backend. Java and Spring Boot were considered because the app is meant to be maintained for years, handles money, and doubles as practice in a widely hired-for backend stack. Python (Monarch) and Node (Copilot) are common among consumer finance startups; Java is common at larger finance companies.

## Decision
The backend uses Java 25 and Spring Boot 4.x with Maven. The frontend stays React and TypeScript, with API types generated from the backend's OpenAPI spec.

## Consequences
- Slower start while learning Java and Spring; accepted.
- Stable, long-lived ecosystem with fewer breaking upgrades; strong refactoring tools; `BigDecimal` for exact money math; built-in passkeys, scheduling, and security in Spring.
- Two languages and toolchains to maintain. Generated API types prevent the two sides from drifting.
- The rank quiz and other small experiments can still use TypeScript.

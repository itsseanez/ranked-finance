# 0008. Swappable AI providers, and the app works without AI

- **Status:** Accepted
- **Date:** 2026-11-19

## Context
Self-hosters may want a different AI provider than Claude: their own OpenAI key, a gateway like OpenRouter, or a local model through Ollama so financial data never leaves their server. Self-hosted apps commonly support a few native providers plus "any OpenAI-compatible endpoint" (base URL, model, key), which covers Ollama, LM Studio, vLLM, OpenRouter, Groq, and others. Spring AI gives a Spring Boot app one chat API across Anthropic, OpenAI, Ollama, and more. The design already computes every number in code, so the AI only explains.

## Decision
- The AI sits behind the `AiClient` interface, implemented with Spring AI.
- Supported configurations: Anthropic, OpenAI, and any OpenAI-compatible endpoint, selected by environment variables.
- The hosted version uses Claude with prompt caching. If Spring AI lacks a Claude feature the hosted version needs, that path uses the official Anthropic SDK behind the same interface.
- AI is optional. With none configured, the Ask panel is hidden and suggestion cards use templated wording.
- v1 builds the context on the server and sends it in the prompt, so tool calling isn't required.

## Consequences
- Prompts must work on models weaker than Claude: short, explicit, and grounded in the numbers sent. The quality bar is checked on Claude and at least one local model.
- Prompt caching and model choice (Haiku vs Sonnet) are hosted-version optimizations, not assumptions in shared code.
- The privacy screen names the configured provider, and says nothing leaves the server when the model is local.
- Nothing that affects rank, plan, or balances may depend on an AI response.

# ADR 0004: AI Tool Calling — Spring AI @Tool Functions vs Separate MCP Server

## Status
Accepted

## Context
The triage agent needs tool-calling capabilities to interact with domain services during ticket triage: searching the knowledge base, retrieving ticket history, and creating escalations.

The architecture rationale (§11) describes the MCP server as "genuinely separate" but "run embedded in-process behind a Spring Profile flag" for free-tier demos. The guardrails (§6) ask us to decide explicitly whether a separate MCP server/client is solving a real problem here.

**Options:**

| Option | Complexity | Testability | Production-readiness |
|--------|-----------|-------------|---------------------|
| **Spring AI `@Tool` functions** (same process) | Low | High — direct unit testing | Good — natural Spring integration |
| **Separate MCP server** (inter-process protocol) | High — second deployable, circuit breaker, separate tests | Medium — needs integration test harness | Better for true microservice extraction |

## Decision
Start with **Spring AI `@Tool` function calling** in the same process. The triage agent calls `@Tool`-annotated methods directly within the Spring context.

## Rationale
1. **Same tool-calling pattern** — Spring AI's `@Tool` annotation exposes methods as callable tools to the LLM. The ChatClient sends tool descriptions in the prompt, the LLM responds with a tool-call request, and Spring AI automatically invokes the method. This is functionally identical to what MCP does, minus the network hop.
2. **Simpler to test** — tools are regular Spring beans, testable with plain unit tests and `@MockitoBean`.
3. **No second deployable** — avoids the cold-start problem (Render free tier: two services = two 30-60s cold starts in sequence).
4. **Extraction path is clean** — if a separate MCP server is needed later (e.g., for the Kubernetes phase where it runs as its own container), the `@Tool` methods can be moved to a separate module with minimal refactoring. The tool interface stays the same.
5. **Guardrails compliance** — §6 says to make this trade-off explicit rather than silently paying the extra complexity.

## Consequences
- The MCP protocol boundary is not demonstrated as a network boundary in Phase 4. If MCP is important for the portfolio, we extract it in a later phase.
- All AI tool functions live in `com.nexus.ai.tools` as Spring `@Service` beans.

# Nexus — Architecture & Component Rationale

*Why each production-grade component exists in this system, and where it fits. This is the reasoning document, not the build plan — every decision below is answerable in an interview with "because X breaks without it," not "because it looks good on a resume."*

---

## 0. The organizing principle

Every component below earns its place by solving a problem Nexus actually has as a **multi-tenant AI SaaS**: tenants must never see each other's data, AI decisions must be explainable and reversible, the system must degrade gracefully when the LLM provider is slow or down, and a single noisy tenant must not be able to starve everyone else. Nothing here is included because "senior projects have it" — each one is the direct answer to one of those four problems, or to the basic mechanics of running a correct, maintainable Spring Boot service.

The components group into ten concerns. Each section says **what it is for in Nexus specifically**, not what it is in general.

---

## 1. Code architecture — how the codebase itself is organized

| Component | Why Nexus needs it |
|---|---|
| **Clean Architecture** | The triage logic (domain rules: when does the AI auto-resolve vs escalate?) must not depend on Spring, Postgres, or the AI provider. If Groq is swapped for a different provider, or Postgres for another store, the core business rule "escalate below 80% confidence" shouldn't change one line. Dependencies point inward: domain has zero framework imports. |
| **Layered design** (Controller → Service → Repository) | Standard separation of HTTP concerns, business logic, and persistence. This is the load-bearing structure everything else (transactions, caching, validation) attaches to correctly. Without it, `@Transactional` boundaries and cache annotations end up in the wrong place — the single most common mistake in unstructured Spring codebases. |
| **DTO layer** | Entities (JPA-managed, lazy-loaded, tenant-scoped) must never leave the service layer directly — serializing an entity to JSON risks leaking another tenant's lazy-loaded relation, exposing internal IDs, or breaking on a `LazyInitializationException`. DTOs are the tenant-safety boundary as much as an API-shape decision. |
| **Design patterns** — used because the domain asks for them, not decoratively: | |
| — *State* | The ticket lifecycle (`NEW → CLASSIFIED → AI_DRAFTED → AUTO_RESOLVED / ESCALATED → IN_PROGRESS → RESOLVED → CLOSED`) has real transition rules — a closed ticket can't be re-escalated. This is a textbook State pattern, not a bag of booleans. |
| — *Strategy* | Escalation confidence thresholds differ by tenant plan tier, and rate-limit policy differs by plan tier too. Strategy lets these vary without an `if/else` ladder growing in the service layer every time a new tier is added. |
| — *Factory* | Notification dispatch (email vs Slack vs webhook, depending on tenant config) is created by a factory keyed on tenant preference — adding a new channel means adding a class, not editing existing code. |
| — *Observer* | Ticket state changes need to fan out to multiple independent listeners (Kafka event publisher, audit logger, metrics counter) without the core ticket service knowing any of them exist. Spring's `ApplicationEventPublisher` implements this cleanly and is also what feeds the Kafka producer described in §6. |
| **State machine / workflow management** | Rather than hand-rolling transition validation, the ticket lifecycle is implemented with Spring Statemachine (or a lightweight enum-based transition table if that's overkill for the scope). This is what actually enforces the State pattern above — it's the difference between "we drew a diagram of the lifecycle" and "the lifecycle is impossible to violate in code." |

---

## 2. API layer — the contract with the outside world

| Component | Why Nexus needs it |
|---|---|
| **REST API design** | Resource-oriented, versioned endpoints (`/api/v1/tenants/{id}/tickets`, `/api/v1/tickets/{id}/escalate`) with correct HTTP verbs and status codes. Versioning in the path from day one costs nothing now and avoids a breaking change later — this is the surface both the dashboard and third-party integrators consume, and once it has external callers, get the contract wrong once and every client breaks. |
| **Validation** (Bean Validation / `@Valid`) | Every inbound DTO is validated before it touches a service method — a ticket with no subject, or a tenant ID that isn't a valid UUID, should never reach the database layer. This is the first line of defense, cheaper than a DB constraint violation. |
| **Global exception handler** (`@ControllerAdvice`) | Centralizes how every error becomes an HTTP response — a validation failure, a not-found tenant, and an AI-provider timeout should each map to a distinct, consistent JSON error shape, not three different ad-hoc try/catch blocks scattered across controllers. |
| **Custom exceptions** | `TenantNotFoundException`, `InsufficientConfidenceException`, `RateLimitExceededException` — each carries the specific context the global handler needs to pick the right status code and message, instead of generic `RuntimeException` that tells the caller nothing. |
| **API idempotency** (`Idempotency-Key` header) | A third-party integrator's webhook retries `POST /tickets` after a network blip on their end — without an idempotency key, that's a duplicate ticket. The endpoint stores the key with its result for a short window and returns the original response on a repeat, instead of creating a second ticket. Standard practice for any external-facing write endpoint, and a detail that shows you've thought about integrators, not just the happy path. |
| **Pagination, filtering, sorting** | A tenant with 10,000 tickets can't have `GET /tickets` return all of them. This is the actual daily-use API for the support dashboard — filter by status/priority, sort by created date, paginate results. Not a demo feature, the primary way agents interact with the system. |
| **OpenAPI / Swagger** | The contract is generated from code, not hand-written and immediately stale. This is also what a hiring manager clicks first — a live, browsable API spec is a stronger signal than a paragraph describing your endpoints. |

---

## 3. Data layer — correctness under concurrency and multi-tenancy

| Component | Why Nexus needs it |
|---|---|
| **Complex JPA queries** | Dashboard aggregates ("tickets resolved by AI this week, by category") aren't simple `findBy` methods — they need joins, grouping, and projections. This is where you show you understand JPA beyond CRUD generation. |
| **Specifications API** | The ticket filter endpoint (status + priority + date range + assignee, any combination optional) is a textbook case for dynamic, composable queries. Hardcoding a method per filter combination doesn't scale; Specifications build the `WHERE` clause from whatever the caller actually sent. |
| **Transactions** | Creating a ticket and writing its audit log entry must succeed or fail together — a half-committed ticket with no audit trail is a data-integrity bug, not an edge case, in a system whose whole pitch is explainability. Publishing the Kafka event is deliberately *not* inside this database transaction — see the Transactional Outbox row below for why that would actually be a bug, not a simplification. |
| **Transactional Outbox pattern** | A JDBC transaction and a Kafka publish can't commit together — there's no distributed transaction spanning Postgres and Kafka, so "publish the event inside the same `@Transactional` method" is the dual-write problem: the DB commit can succeed while the Kafka publish fails (or vice versa), silently losing or duplicating an event. The fix used here: write the event into an `outbox_events` table in the *same* transaction as the ticket write (that part *is* atomic — it's one database), then a scheduled poller reads unpublished rows and publishes them to Kafka, marking them sent. This is what makes the "must succeed or fail together" guarantee actually true, instead of just asserted. |
| **Optimistic locking** (`@Version`) | Two agents open the same escalated ticket at once — one resolves it while the other is mid-edit. Optimistic locking is what stops the second write from silently overwriting the first, without taking a heavyweight row lock on every read. |
| **Flyway** | Every schema change (adding the `confidence_score` column, adding an RLS policy) is a versioned, repeatable migration — not a manual `ALTER TABLE` someone forgot to run in staging. This is also what makes the CI pipeline able to spin up a real schema in a test container automatically. |

---

## 4. Caching — cost and latency control

| Component | Why Nexus needs it |
|---|---|
| **Redis (Upstash, free tier)** | Two distinct jobs: rate-limit counters (§7) and response caching. Both need a fast, shared, TTL-capable store outside the JVM heap so it works correctly across multiple instances. |
| **Spring Cache abstraction** | Repeated or near-identical AI queries (two customers asking "how do I reset my password" against the same knowledge base) don't need a fresh Groq call every time — `@Cacheable` on the RAG lookup path cuts both latency and the free-tier rate-limit burn described in the earlier stack breakdown. This is a genuinely non-obvious optimization, not caching for caching's sake — it directly protects the scarce resource (LLM API quota). |

---

## 5. Security — the layer this product is actually sold on

| Component | Why Nexus needs it |
|---|---|
| **JWT authentication** | Stateless auth so the API scales horizontally without sticky sessions — any instance can validate any request. |
| **OAuth2 login** | Real B2B SaaS products don't make users invent another password — "Sign in with Google/Microsoft" via Spring Security's OAuth2 client is the actual onboarding flow tenants expect, and it's a materially different implementation from JWT-only auth (token exchange, provider callback, account linking). |
| **RBAC** | `OWNER`, `ADMIN`, `AGENT` roles per tenant — an agent can view and resolve tickets but not change billing or invite users. Enforced via `@PreAuthorize` at the method level, not just hidden UI buttons. |
| **Multi-tenancy strategy: shared schema + Postgres RLS** | Deliberately not schema-per-tenant or database-per-tenant — those don't fit a free-tier single Neon project anyway, and shared-schema-with-RLS is what most real B2B SaaS actually runs at this scale (schema-per-tenant stops scaling cleanly past a few hundred tenants). RBAC controls *what a role can do*; RLS controls *which rows any query can ever see*, even if a service method has a bug — it's the last line of defense, enforced by Postgres itself, not by remembering to add `WHERE tenant_id = ?` everywhere. |
| **RLS + connection pooling — the gotcha that actually breaks multi-tenancy** | Both HikariCP (app-side) and Neon's pooler (Postgres-side) reuse physical connections across requests. If tenant context is set with a session-level `SET app.tenant_id = '...'`, a pooled connection can carry tenant A's setting into a request that's actually serving tenant B — a real, well-documented class of cross-tenant leak, not a hypothetical. The fix: set tenant context with `SET LOCAL` **inside the same transaction** as the query, so Postgres resets it automatically at transaction end regardless of what the pool does with the physical connection afterward. This gets wired once, correctly, at the transaction-interceptor level — not left to each service method to remember. | |

---

## 6. Configuration and environments

| Component | Why Nexus needs it |
|---|---|
| **Spring Profiles** | `local` (Docker Compose Kafka, local Postgres), `dev` (Neon + Upstash free tier), and a documented `prod` profile (what it would look like on AWS) — the same codebase, different externalized config per environment. This is what proves the "how this scales to real production" story isn't just prose, it's a config file away. |
| **`@ConfigurationProperties`** | Typed, validated configuration (rate-limit thresholds per plan tier, confidence thresholds, Groq API settings) instead of scattered `@Value("${...}")` strings — one class, IDE-autocompleted, fails fast at startup if a required property is missing. |

---

## 7. Observability — because "it works on my machine" isn't a production claim

| Component | Why Nexus needs it |
|---|---|
| **Structured logging** (JSON via Logback) | Logs need to be machine-parseable by whatever aggregates them — a plain-text log line is useless once you have more than one instance. Every log line carries `tenantId` and `traceId` so an incident can be traced to exactly one tenant's request. |
| **Audit logging** | Distinct from application logs — this is the permanent, queryable record of *every AI decision*: what the model saw, what it decided, what confidence score it produced, whether a human overrode it. This is the actual product feature that makes "explainable AI" a real claim instead of marketing copy. |
| **Confidence score derivation (not raw self-report)** | Asking the model "how confident are you, 0-100" and trusting the number is a known failure mode — LLMs are notoriously miscalibrated when self-reporting confidence, tending to sound sure regardless of actual correctness. Nexus derives confidence instead from signals it can actually verify: whether structured-output parsing succeeded cleanly, the RAG retrieval similarity score (how well the knowledge base actually matched the query), and optionally a second, independent verification prompt checked for agreement with the first. Small design choice, outsized interview payoff — it shows awareness of a specific, real LLM-engineering pitfall most portfolio projects never consider. |
| **Micrometer** | The vendor-neutral metrics facade — instrument once (`ticket.triage.duration`, `ai.tokens.consumed`), export anywhere. This is what makes swapping Prometheus for another backend a config change, not a rewrite. |
| **Quota observability (free-tier usage gauges)** | Because this entire stack runs on hard-capped free tiers (Groq daily requests, Neon CU-hours, Upstash command count), Micrometer gauges track consumption against each cap, visible on the same Grafana dashboard as everything else. This is observability applied to the project's own operating constraint, not just the application — and it's what actually protects the "zero cost" requirement from being silently violated by an enthusiastic load test or a busy demo day. |
| **Prometheus** | Scrapes and stores those metrics. The concrete question it answers: "how many tickets is the AI auto-resolving per hour, and is p99 triage latency creeping up?" |
| **Grafana** | Turns those metrics into the one dashboard screenshot that proves, in an interview, that you didn't just build this — you watched it run. |
| **Distributed tracing** (OpenTelemetry) | A single ticket triage touches four things: the API, the Postgres query, the MCP tool call, and the Groq API call. Tracing is what shows *which one* is slow when p99 latency spikes — without it, "it's slow" has no next step. |

---

## 8. Asynchronous and event-driven processing

| Component | Why Nexus needs it |
|---|---|
| **Kafka** (self-hosted via Docker for local dev; a single-broker deployment inside the kubeadm cluster for the Kubernetes phase; documented path to Confluent Cloud / MSK for real production) | The ticket lifecycle is a real event stream — `ticket.created → ticket.classified → ticket.escalated/resolved → ticket.closed`. Events are published via the outbox poller above, not directly from request-handling code. Publishing them as events (rather than direct method calls between services) means new consumers — a notification service, a billing/usage-metering service, analytics — attach without ever touching the core triage code. This is the actual argument for event-driven design, not "Kafka because Kafka." |
| **Idempotent consumers** | Kafka guarantees at-least-once delivery, not exactly-once — a consumer can see the same `ticket.escalated` event twice after a rebalance. Every consumer checks a `processed_events` dedup key before acting, so a duplicate delivery doesn't send a duplicate Slack notification or double-count a metric. Easy to skip, and exactly the kind of bug that only shows up under real failure conditions — worth building correctly the first time rather than having a demo embarrassingly double-fire a notification. |
| **Async processing** (`@Async`, or a dedicated worker consuming Kafka) | An LLM call takes 1–5 seconds with unpredictable tail latency. Blocking an HTTP request thread on that is what takes down the whole service under load. The ticket is accepted synchronously, triage happens asynchronously, and the client gets the result via polling or a webhook — this is the mechanical reason async matters here, not a checkbox. |
| **Scheduled jobs** (`@Scheduled`) | Three concrete uses: the outbox poller described above (runs every few seconds — it's on the critical path for event delivery latency, not a nightly batch job); nightly re-indexing of the tenant's knowledge base into pgvector; and a sweep job that finds tickets stuck in `AI_DRAFTED` past an SLA window and auto-escalates them. All three are genuine background-work needs, not invented ones. |

---

## 9. Resilience — what happens when a dependency fails

| Component | Why Nexus needs it |
|---|---|
| **Circuit breaker** (Resilience4j) | Wraps every call to Groq. If the provider is down or rate-limiting hard, the circuit opens, requests fail fast with a clear "AI temporarily unavailable, ticket queued" response instead of every thread hanging until timeout and taking the whole service down with it. This is the single most concrete "production thinking" detail in the whole project — the honest answer to "what happens when your AI vendor has an outage." |
| **Retry mechanism** | Transient failures (a dropped connection, a momentary 503) are retried with backoff before the circuit breaker even engages — most failures are transient and shouldn't need the heavier fallback path at all. |
| **Feature-flag kill switch** | A single `@ConfigurationProperties`-backed toggle (`nexus.ai.auto-resolve-enabled`) disables autonomous ticket resolution at runtime, without a redeploy, falling back to "AI drafts, human always approves." This is the honest, minimal answer to "what if the AI starts making bad calls in front of a real user" — a real operational safety valve, not just a resilience talking point. |

---

## 10. Delivery — build, ship, run

| Component | Why Nexus needs it |
|---|---|
| **Docker** | Multi-stage build (compile stage, slim runtime image) — this is what makes the app deployable identically on your laptop, in CI, and on Render. |
| **Kubernetes** | Run for real on a self-managed `kubeadm` cluster (master + 2 workers on VMware) rather than documented-only — `Deployment`, `Service`, `HorizontalPodAutoscaler`, MetalLB (no cloud load balancer on bare VMs) and ingress-nginx as the entry point. Liveness and readiness probes are deliberately different checks, not the same endpoint twice: liveness asks "is the JVM alive" (restart the pod if not), readiness asks "can this pod actually serve traffic right now" (is Postgres/Redis/Kafka reachable — pull it out of load-balancing rotation if not, without killing it). A single-broker Kafka runs *inside* the cluster for this phase rather than bridging out to the laptop's Docker Compose instance — simpler networking, and it's what a real deployment would do anyway. |
| **CI/CD** (GitHub Actions) | Lint → test (with Testcontainers spinning up real Postgres) → build → Docker image → deploy to Render, on every push. The green badge on the README is the proof this isn't a one-time `mvn spring-boot:run` that happened to work once. |
| **Unit & integration testing** | Unit tests cover the domain logic in isolation (Clean Architecture is what makes this possible without mocking half of Spring). Integration tests use Testcontainers against real Postgres and Redis — not H2, because H2 doesn't enforce the same constraints, doesn't have pgvector, and won't catch a real RLS policy bug. |

---

## 11. How the AI-specific pieces sit on top of all of this

Everything above is the general-purpose production skeleton any serious Spring Boot service needs. Spring AI, MCP, and the agentic triage logic sit *inside* this skeleton, not beside it:

- The MCP server (exposing `search_knowledge_base`, `get_ticket_history`, `create_escalation`) is architected as a genuinely separate service — but *run* embedded in-process behind a Spring Profile flag for the live free-tier demo, so a cold Render instance doesn't compound into two sequential 30-60s cold starts (API waking up, then waiting on the MCP server to wake up too). Locally, and on the Kubernetes deployment, it runs as its own container reached over the network — which is what actually proves the protocol boundary is real, not just a class boundary wearing an MCP label.
- The triage agent's LLM call is wrapped in the same circuit breaker and retry logic as any other external dependency — the architecture doesn't treat "AI call" as a special case, which is itself the point: a mature system doesn't have a separate, less-disciplined code path for the AI feature.
- Confidence scores and AI decisions flow through the same audit-logging and Kafka event pipeline as every other domain event.

That consistency — the AI feature isn't bolted on with different rules — is the detail most portfolio projects get wrong, and the one worth stating explicitly in an interview.

---

## Appendix — full component → layer map

| Layer (from the 13-layer model) | Components from this document |
|---|---|
| APIs & backend logic | Clean Architecture, Layered Design, DTOs, REST design, Validation, Global Exception Handler, Custom Exceptions, Pagination/Filtering/Sorting, OpenAPI, Design Patterns, State Machine |
| Database & storage | Complex JPA Queries, Specifications API, Transactions, Optimistic Locking, Flyway |
| Caching & CDN | Redis, Spring Cache |
| Auth & permissions | JWT, OAuth2 Login, RBAC |
| Security & RLS | (RBAC above) + Postgres RLS (from earlier plan) |
| — | Spring Profiles, `@ConfigurationProperties` (cross-cutting, enables every environment-specific choice above) |
| Error tracking & logs | Structured Logging, Audit Logging |
| — | Micrometer, Prometheus, Grafana, Distributed Tracing (cross-cutting observability) |
| Load balancing & scaling | Kafka, Async Processing, Scheduled Jobs |
| Availability & recovery | Circuit Breaker, Retry Mechanism |
| Hosting & deployment / Cloud & compute | Docker, Kubernetes |
| CI/CD & version control | GitHub Actions, Unit & Integration Testing |

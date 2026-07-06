# Nexus — Master Project Specification

*This is the single source of truth for the Nexus project. If you are an AI coding agent (Antigravity / Claude Opus 4.6) working on this codebase: read this entire document before writing any code. If something you're about to build contradicts this document, stop and flag it rather than silently deviating. If a decision genuinely needs to change, it gets recorded as an ADR (§9) — it doesn't just happen in code.*

---

## 1. Project overview

**Nexus** is a multi-tenant, AI-powered customer support SaaS. Multiple companies (tenants) onboard, upload their support knowledge base, and get an AI agent that triages incoming tickets: classifies category/priority, searches the knowledge base, drafts a reply, and either auto-resolves (if confident) or escalates to a human agent with full reasoning attached.

**Core constraints that shape every technical decision below:**
1. Tenant data must never leak across tenants (multi-tenancy, RLS)
2. Every AI decision must be explainable after the fact (audit logging)
3. The system must degrade gracefully when the LLM provider is slow/down (circuit breaker, retry, fallback)
4. One noisy tenant must not starve others (rate limiting)

**This is a portfolio project built to demonstrate Solutions/Software Development Architect-level judgment** — not just feature delivery, but correct sequencing, tested code, honest scope (real vs. documented), and disciplined engineering process. The process discipline in this document is as much a deliverable as the code.

---

## 2. Tech stack (verified free-tier, zero cost)

| Concern | Technology | Notes |
|---|---|---|
| Language/framework | Java 21 (LTS) + Spring Boot 3.x + Maven | |
| AI orchestration | Spring AI (ChatClient, RAG advisor, MCP client) | |
| LLM inference | Groq (OpenAI-compatible API, Llama 3.3 70B / GPT-OSS) | Free, no card, rate-limited — design around it |
| Database | Neon Postgres (pgvector extension enabled) | 0.5 GB / 100 CU-hrs free |
| Cache / rate-limit store | Upstash Redis | 256 MB / 500K commands/month free |
| Object storage | Backblaze B2 | 10 GB free, no card required |
| Messaging | Kafka — self-hosted via Docker Compose (dev/demo); documented migration path to Confluent Cloud / MSK for real prod | |
| Auth | Spring Security — JWT + OAuth2 login | |
| Migrations | Flyway | |
| Resilience | Resilience4j (circuit breaker, retry) | |
| Observability | Micrometer → Prometheus → Grafana; OpenTelemetry tracing; Sentry for error tracking | |
| Hosting (API) | Render free web service | Spin-down after 15 min idle — document this openly |
| Hosting (frontend) | Vercel / Cloudflare Pages | |
| Frontend | React + TypeScript + Vite + TanStack Query + Tailwind | |
| CI/CD | GitHub Actions | |
| Containers | Docker | |
| Orchestration | Kubernetes — real 3-node `kubeadm` cluster on VMware VMs (master + 2 workers), MetalLB + ingress-nginx | Spun up on-demand for the K8s phase/demo, not run continuously — see §8 |
| Testing | JUnit 5, Mockito, Testcontainers (Postgres, Redis, Kafka) | |

---

## 3. Architecture

**Style:** Clean Architecture *within* each feature module, package-by-feature at the top level, enforced by Spring Modulith (ADR 0002). Not package-by-layer globally — Modulith's `ApplicationModules.verify()` treats top-level packages as module boundaries, so domain/application/infrastructure/api are nested inside each feature module, not siblings at the root.
```
com.nexus
 ├── ticket/              # feature module — domain/application/infrastructure/api nested inside
 │    ├── domain/           # zero Spring/JPA imports, verified per-module
 │    ├── application/
 │    ├── infrastructure/
 │    └── api/
 ├── tenant/
 ├── notification/         # the Phase 5 extraction — still a Modulith module here, Spring Boot service later if actually pulled out
 ├── ai/                    # Spring AI, MCP client, RAG
 └── shared/                # cross-module kernel: shared value objects, nothing module-specific
```

**Rule for the coding agent:** `domain` package must compile with zero framework dependencies. If you find yourself importing `jakarta.persistence` or `org.springframework` into `domain`, stop — that class belongs in `infrastructure` or `application`, or the domain model needs a plain interface that infrastructure implements.

**Design patterns used because the domain requires them** (not decoratively):
- **State** — ticket lifecycle (`NEW → CLASSIFIED → AI_DRAFTED → AUTO_RESOLVED|ESCALATED → IN_PROGRESS → RESOLVED → CLOSED`), enforced via a state machine, not booleans
- **Strategy** — confidence thresholds and rate-limit policy vary by tenant plan tier
- **Factory** — notification channel creation (email/Slack/webhook) keyed on tenant config
- **Observer** — ticket state changes fan out to Kafka publisher, audit logger, metrics counter via `ApplicationEventPublisher`, without the ticket service knowing about any of them

---

## 4. Full component list and why each exists

*(Full rationale already written out in `nexus-architecture-rationale.md` — the agent should read that file too. Summary table below for quick reference.)*

| Category | Components |
|---|---|
| Code structure | Clean Architecture, layered design, DTO layer, design patterns, state machine |
| API | REST design, validation, global exception handler, custom exceptions, pagination/filtering/sorting, Specifications API, OpenAPI/Swagger |
| Data | Complex JPA queries, transactions, optimistic locking, Flyway |
| Caching | Redis, Spring Cache |
| Security | JWT, OAuth2 login, RBAC, Postgres row-level security |
| Config | Spring Profiles, `@ConfigurationProperties` |
| Observability | Structured logging, audit logging, Micrometer, Prometheus, Grafana, distributed tracing |
| Async/events | Kafka, async processing, scheduled jobs |
| Resilience | Circuit breaker, retry |
| Delivery | Docker, Kubernetes, CI/CD |
| Quality | Unit + integration testing (TDD — see §7) |
| AI-specific | Spring AI, MCP server/client, RAG over pgvector, Groq integration |

---

## 5. Build order (phases)

**Principle: each phase produces something real and runnable before the next phase adds to it. Never build against a mock of a lower layer.**

| Phase | Deliverable | Gate to move on |
|---|---|---|
| 0 | Repo scaffold, Maven, Spring Profiles skeleton, Docker Compose (Postgres, Redis, Kafka) | `docker compose up` gives a clean local environment |
| 1 | Domain model, Flyway migrations, RLS policies | Migrations run clean; a manual cross-tenant query returns zero rows |
| 2 | Core REST API: DTOs, validation, exception handling, pagination/filtering/Specifications | Full CRUD ticket API works in Postman, unauthenticated |
| 3 | Security: JWT, OAuth2, RBAC | Same API now requires auth; role checks enforced and tested |
| 4 | Spring AI + Groq + MCP tool server + RAG | A real ticket gets triaged end-to-end by the agent |
| 5 | State machine, Kafka events, async processing, scheduled jobs | Ticket lifecycle events are published and consumed; nothing blocks the request thread |
| 6 | Redis caching, circuit breaker + retry, rate limiting | Killing the Groq connection doesn't take down the API; rate limits are enforced per tenant |
| 7 | Structured + audit logging, Micrometer/Prometheus/Grafana, tracing | A real Grafana dashboard shows real triage metrics |
| 8 | Testing formalized (this runs *throughout*, not just here — see §7) | Coverage on domain + integration layers meets the bar in §7 |
| 9 | Docker images, GitHub Actions CI/CD, Render deploy | Green pipeline, live URL |
| 10 | Kubernetes: kubeadm cluster, MetalLB, ingress-nginx, Deployments/HPA, probes | App reachable via ingress on your VMware cluster; HPA demonstrably scales under load |
| 11 | README, architecture diagrams, k6 load test, demo recording | A stranger can understand and run this from the README alone |

**No phase is "done" until its own tests pass and the previous phase's tests still pass.**

---

## 6. Git workflow, branching, and commits

Solo project, but disciplined as if a team depended on `main` always being deployable — this is what actually prevents the "went downhill" failure mode from your last project.

**Branching model: trunk-based, short-lived feature branches.**
- `main` is always green and always deployable. Nothing merges to it without CI passing.
- One branch per unit of work, named `<type>/<short-description>`, e.g. `feat/ticket-state-machine`, `fix/rls-tenant-leak`, `chore/flyway-baseline`.
- Branches live days, not weeks. If a branch is still open after a phase in §5 is supposed to be done, that's a signal the unit of work was too big — split it.
- Merge via PR even solo — opening a PR against yourself forces CI to run and gives you a diff to actually re-read before merging, which catches more than you'd expect.

**Commit convention: Conventional Commits.**
```
<type>(<scope>): <short summary>

<optional body — why, not what>
```
Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`. Scope is the package/module touched, e.g. `feat(ticket): add optimistic locking to escalation flow`.

**Rules for the agent specifically:**
- One logical change per commit. If a commit touches the domain model *and* the API layer *and* adds a migration, it should usually be three commits, not one.
- Never commit generated code, `.env` files, or secrets. `.env.example` gets committed with placeholder values; `.env` is gitignored.
- Never `git commit --amend` or force-push on `main`.
- Write the commit message to describe *intent*, not a changelog of files touched.

**Versioning:** Semantic Versioning (`v0.x.y` while pre-launch). Tag `main` at the end of every phase in §5 (`v0.1.0` after phase 1, etc.) — this gives you a working checkpoint to roll back to if a later phase breaks something, and a clean story in an interview: "here's the commit history showing the system built up in verifiable stages."

---

## 7. Test-driven development — the actual workflow

This is the part most likely to get skipped under agentic-IDE velocity, so it's explicit:

**Red → Green → Refactor, applied at two levels:**
1. **Domain logic** (state machine transitions, confidence-threshold strategy, DTO mapping): write the failing unit test first, then the implementation. These are fast, no-Spring-context tests — there's no excuse to skip TDD here.
2. **API/integration behavior** (an endpoint, a Specification query, an RLS boundary): write a Testcontainers-backed integration test describing the expected behavior *before* implementing the controller/query. Slower, but this is exactly where RLS and tenant-isolation bugs hide — the ones that don't show up until it's a real incident.

**Definition of done for any feature (agent must self-check this before considering work complete):**
- [ ] Unit tests for domain logic exist and pass
- [ ] Integration test exists for the API-level behavior and passes against Testcontainers (not H2)
- [ ] No `TODO`/`FIXME` left unaddressed without a tracked issue
- [ ] Global exception handler covers the new failure modes this feature introduces
- [ ] If a new entity/column was added, a Flyway migration exists and is committed
- [ ] If tenant-scoped data is involved, an RLS-bypass test explicitly proves cross-tenant access fails
- [ ] Structured log lines include `tenantId`/`traceId` where relevant
- [ ] OpenAPI annotations updated if the endpoint contract changed
- [ ] This spec document updated if the feature changes an architectural decision (not just implements one)

**Minimum bar, not a target to inflate:** every public service method in `application` and every state transition in `domain` has at least one test. Don't chase a coverage percentage — chase "would this test have caught the bug that took the last project down."

---

## 8. CI/CD pipeline

GitHub Actions, triggered on every push and PR:

1. **Lint/format check** — fails fast, cheapest check first
2. **Unit tests** — domain layer, no external dependencies, seconds to run
3. **Integration tests** — Testcontainers spins up real Postgres/Redis/Kafka in the CI runner
4. **Build** — Docker multi-stage image
5. **(main branch only)** Push image → deploy to Render

Branch protection on `main`: PRs require the pipeline to pass before merge is allowed — no exceptions, including for the agent's own commits.

**Kubernetes is intentionally outside the automated pipeline.** The kubeadm cluster is your own VMware VMs, spun up on-demand (see §2/§5) — not something CI deploys to automatically. Document the manual `kubectl apply` steps in the README instead of faking automation to infrastructure that isn't always running.

---

## 9. Architecture Decision Records (ADRs)

Every non-trivial decision (choice of Groq over another provider, self-hosted Kafka vs. a managed alternative, RLS vs. application-level tenant filtering) gets a short ADR in `/docs/adr/NNNN-title.md`:

```
# NNNN. <Decision title>
Status: Accepted
Context: <what problem forced this decision>
Decision: <what was chosen>
Consequences: <what this makes easier, what it makes harder>
```

**This is what stops the agent (or you, six weeks from now) from silently re-litigating settled decisions mid-project** — if the reasoning is written down, "should we switch to X" has to argue against the recorded ADR explicitly, not just happen because it seemed convenient in the moment.

---

## 10. Instructions specifically for the coding agent

If you are Antigravity/Claude Opus 4.6 executing against this spec:

1. **Work one phase of §5 at a time, in order.** Don't jump ahead to Kubernetes manifests while Phase 2's API is still unfinished.
2. **Write the test before the implementation**, per §7. If you generate implementation code first, generate the test immediately after in the same working session, before moving to the next feature — never leave untested code committed.
3. **Small, frequent commits** per §6 — not one giant commit per phase. If you're about to write a commit message that needs three paragraphs to describe, it should have been several commits.
4. **Don't silently change the tech stack in §2.** If Groq's rate limits genuinely block progress and a substitution seems necessary, stop and propose it rather than switching providers mid-implementation.
5. **Keep `domain/` framework-free**, per §3. This is the one architectural rule most likely to get violated under time pressure — check every new class in `domain/` for stray Spring/JPA imports before committing.
6. **Update this document** when a decision changes, and add an ADR (§9) for why. This file is the contract between sessions — if it's stale, the next session (yours or the human's) starts from wrong assumptions.
7. **Ask rather than assume** when a requirement is genuinely ambiguous — silently picking a plausible interpretation and building a full feature on it is the fastest way to waste a session's work.

---

## 11. Definition of "project complete" for the portfolio milestone

- All 11 phases in §5 done, gated as specified
- CI green on `main`, with a badge in the README
- Live demo URL (Render) reachable
- Kubernetes deployment demonstrated on the VMware cluster with a recorded HPA-scaling-under-load clip
- One real Grafana dashboard screenshot with real triage metrics
- One k6 load test result, honestly reported (including where it breaks)
- README that lets a stranger understand the system, run it locally, and see the architecture diagrams in under five minutes of reading
- ADR log showing at least the 5-6 biggest decisions made across this project

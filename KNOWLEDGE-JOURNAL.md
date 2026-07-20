# Knowledge Journal

The full, detailed explanations from every unit of this project — the before-code context, the after-code walkthrough, decisions that matter, and what could go wrong. This is the companion to `LEARNING-LOG.md` (which is your own-words recall). Use this as reference material when you need the details again.

---

## Phase 0 — Repo Scaffold

### Unit 1: `CURRENT_STATE.md` and `SETUP_CHECKLIST.md`

#### Before code — what and why
These two files are the project's memory between sessions. `CURRENT_STATE.md` records what phase we're in, what's done, what's blocked, and which reference docs are present — so any session (or any person) that opens this repo knows exactly where things stand without re-reading the whole playbook. `SETUP_CHECKLIST.md` tracks every external credential the project will eventually need, when it's needed, and whether it's been provided — so we never hit a surprise mid-phase "I need an API key you don't have." We create them now, at the very start of Phase 0, because the playbook's Session Start Protocol (§1) reads them first thing — if they don't exist, no future session can start cleanly.

#### Files created
- `CURRENT_STATE.md` — from the template in playbook §7
- `SETUP_CHECKLIST.md` — from the template in playbook §7

#### Decisions that matter
1. **"In progress right now" starts populated, not empty.** The §7 template has a placeholder. We filled it with the actual current work so anyone opening the repo sees the real state, not a template.
2. **All three reference docs marked `present`.** We actually checked — `ls` on `/docs/` confirmed all three files exist. If any were missing, we'd write `absent` and note it's non-blocking per §0's resilience clause.
3. **Every row in SETUP_CHECKLIST is `pending`, and that's correct.** Phase 0 needs zero credentials (§4). The first credential ask happens at Phase 1 start (Neon Postgres). Logging them all now means you can see the full road ahead.

#### What could go wrong
If someone hand-edits one of these files and introduces a typo in a status field (e.g., `providd` instead of `provided`), there's no validation — these are plain Markdown, not structured data. The playbook relies on discipline, not tooling, to keep them accurate.

---

### Unit 2: Maven Parent POM (`pom.xml`)

#### Before code — what and why
A single file — `pom.xml` at the repo root — that tells Maven "this project exists, it uses Java 21 and Spring Boot 3, and it has these sub-modules." It contains no application code. It's purely build configuration.

Every Java file we write from here on needs to be compiled, and Maven is the tool that does that. Without this file, there's no project — no `mvn test`, no `mvn spring-boot:run`, nothing. It's the foundation everything else sits on.

Think of it like a table of contents for a book that also specifies what language the book is written in and what printing press to use. The chapters (modules like `ticket`, `tenant`, `ai`) don't exist yet — we'll create their individual POMs in the next units — but this parent POM declares they will exist and what they all share.

#### File created
- `pom.xml` (repo root) — the parent/aggregator POM

#### Decisions that matter

**1. Why does `<parent>` point to `spring-boot-starter-parent` instead of standing alone?**
By inheriting from Spring Boot's parent POM, we get ~200 dependency versions pre-locked (the right version of Jackson, Hibernate, Logback, etc. that are tested together with Spring Boot 3.4.1). Without this, we'd have to manually pick compatible versions for every library — a recipe for obscure runtime errors from version mismatches.

**2. Why `<packaging>pom</packaging>`?**
This parent POM doesn't produce a JAR — it's just a container that says "build these child modules and here are the shared settings." The `pom` packaging type tells Maven: "don't try to compile Java code here, just manage children."

**3. Why `<dependencyManagement>` with BOMs instead of `<dependencies>`?**
This is the most important distinction in the file. `<dependencies>` would force every child module to include Spring AI and Modulith whether it needs them or not. `<dependencyManagement>` only **locks the versions** — each child module still chooses what it actually uses. Think of it as "if any child wants Spring AI, they get version 1.0.0" versus "every child must have Spring AI."

**4. Why a single `nexus-app` module instead of separate Maven modules per feature (ticket, tenant, ai, etc.)?**
The master spec (§3) says to use Spring Modulith, which enforces module boundaries through **packages**, not Maven modules. Having `com.nexus.ticket` and `com.nexus.tenant` as packages within one module lets Modulith's `ApplicationModules.verify()` catch illegal cross-module access. Splitting them into separate Maven modules would add build complexity without adding enforcement we don't already get from Modulith + ArchUnit.

**5. The key versions chosen:**
- **Java 21** — the current Long Term Support (LTS) release, as specified by the master spec §2
- **Spring Boot 3.4.1** — latest stable in the 3.x line
- **Spring AI 1.0.0** — the GA release that works with Spring Boot 3.4.x
- **Spring Modulith 1.3.1** — compatible with Spring Boot 3.4.x

#### What could go wrong
The `nexus-app` module doesn't exist yet — if you ran `mvn validate` right now, Maven would fail with "Could not find child module nexus-app." That's expected; the child module POM is the next unit. This file is temporarily in a broken state until then.

---

### Unit 3: Child Module POM (`nexus-app/pom.xml`)

#### Before code — what and why
The parent POM locked **versions** (via `<dependencyManagement>`). This child POM declares **what we actually use**. Think of the parent as a restaurant's price list and this child as your actual order — the price list exists so everything has a consistent price, but you still have to say what you want.

For Phase 0, we need enough to: start a Spring Boot web app (even if it does nothing yet), run the ArchUnit domain-purity test, and set up the test infrastructure (JUnit 5). We also declared database dependencies (JPA, PostgreSQL, Flyway) because they're structural — they define what kind of application this is, even though they won't be used until Phase 1.

#### File created
- `nexus-app/pom.xml` — the child module POM

#### Decisions that matter

**1. Why no `<version>` on most dependencies?**
The parent POM inherits from `spring-boot-starter-parent`, which already locks ~200 library versions to ones tested together with Spring Boot 3.4.1. Only `archunit-junit5` (version `1.3.0`) has an explicit version because ArchUnit isn't part of Spring Boot's managed set.

**2. Why is PostgreSQL `<scope>runtime</scope>` but JPA isn't?**
- No scope (default = `compile`) → available everywhere — your code can import its classes
- `runtime` → available when running the app, but NOT when compiling. Code never imports `org.postgresql` directly — it talks through JPA/Hibernate, which uses the driver internally. Making it `runtime` enforces nobody writes raw JDBC bypassing JPA.
- `test` → only available during test execution. ArchUnit, Testcontainers, etc. never ship in production.

**3. Why Testcontainers already?**
The dependency declaration is cheap (a line). Having it ready means Phase 1's RLS integration tests just use it — no mid-phase reconfiguration. Containers don't start until a test asks for one.

**4. Why `spring-boot-maven-plugin` in `<build>`?**
This makes `mvn spring-boot:run` work and packages the app into a "fat JAR" (one file = app + all dependencies + embedded web server). Without it, you'd get a normal JAR that can't run on its own.

#### What could go wrong
We declared `spring-boot-starter-data-jpa` which requires a database connection on startup. Running the app right now without a database URL would fail. Docker Compose (upcoming unit) provides Postgres, and Spring Profiles will configure the connection. Until then, `mvn spring-boot:run` would crash — that's expected, not a bug.

---

### Unit 4: Spring Boot Application Entry Point (`NexusApplication.java`)

#### Before code — what and why
Every Spring Boot application needs exactly one class with a `main` method that starts the whole thing — the ignition key. It lives at `com.nexus` (the root package), and Spring Boot automatically scans every sub-package beneath it for components. Placing it at the root means all feature modules (`ticket`, `tenant`, `ai`, etc.) are discovered automatically.

#### Files created
- `nexus-app/src/main/java/com/nexus/NexusApplication.java` — the entry point class
- `nexus-app/src/main/resources/application.yml` — minimal base configuration

#### Decisions that matter

**1. `@SpringBootApplication` — one annotation doing three jobs:**
- `@Configuration` — "this class can define beans (objects Spring manages)"
- `@EnableAutoConfiguration` — "look at my dependencies and configure yourself automatically"
- `@ComponentScan` — "scan my package and everything under it"

This is why the class is at `com.nexus` and not deeper — scanning starts from this class's package and goes downward.

**2. `SpringApplication.run()` — what happens in that one line:**
Creates the Spring container, reads `application.yml`, auto-configures based on classpath dependencies (JPA → Hibernate, Web → embedded Tomcat), scans for components, starts everything.

**3. YAML over `.properties`:**
Both work. YAML is more readable for nested config, and we'll have a lot of it (database, security, AI providers).

**4. No feature packages created yet.**
They'll appear naturally as we add code. Empty packages in Java serve no purpose — they'd just be directories with nothing in them.

#### What could go wrong
Running the app now would crash — JPA auto-configuration tries to connect to a database we haven't configured yet. Docker Compose + Spring Profiles (next units) will fix this. This is expected, not a bug.

---

### Unit 5: Docker Compose + `.env.example`

#### Before code — what and why
The app crashes without a database. Docker Compose lets us run Postgres, Redis, and Kafka as containers with one command (`docker compose up`) — no native installation needed. `.env.example` is a committed map of every environment variable, with empty values only (real values go in gitignored `.env`).

#### Files created
- `docker-compose.yml` — three services: Postgres+pgvector, Redis, Kafka (KRaft mode)
- `.env.example` — every env var grouped by phase, empty placeholders only

#### Decisions that matter

**1. `${POSTGRES_PASSWORD:-nexus_local}` syntax:** Uses `.env` value if present, otherwise falls back to the default. Means `docker compose up` works without creating `.env` first — safe local-only defaults.

**2. Health checks on every service:** Without them, Docker says "running" the instant a container starts, even if the service inside isn't ready for connections yet. Health checks verify actual readiness (e.g., `pg_isready` for Postgres, `redis-cli ping` for Redis).

**3. Kafka in KRaft mode (no ZooKeeper):** Kafka 3.x manages its own metadata without a separate ZooKeeper service. One fewer container, fewer failure points. `KAFKA_PROCESS_ROLES: broker,controller` = this single node does both jobs.

**4. Named volumes (`nexus-pg-data`, `nexus-redis-data`):** Persist data across container restarts. `docker compose down` keeps them; `docker compose down -v` deletes them for a fresh start.

**5. `start_period: 30s` on Kafka only:** Kafka takes ~20-30s to boot (initialize logs, elect controller). Without this, Docker would mark it unhealthy before it even finishes starting.

**6. `.env.example` values are all empty:** Per §2.1 — "never fake values." This file is a map showing what exists and when you'll need it. Real values go in `.env`.

#### What could go wrong
The Kafka health check script path (`/opt/kafka/bin/kafka-broker-api-versions.sh`) is specific to the `apache/kafka:3.8.0` image. A future image version could move it, causing a false "unhealthy" status — a config problem, not a Kafka problem.

---

### Unit 6: Spring Profiles (`application.yml` + `application-dev.yml`)

#### Before code — what and why
The app crashes on startup because JPA tries to connect to a database we've never told it about. Spring Profiles solve this: each environment (dev, test, prod) gets its own `application-{profile}.yml` file, and the base `application.yml` holds settings shared by all. This unit finally connects the app to the Docker Compose Postgres.

#### Files modified/created
- `application.yml` (modified) — expanded with shared JPA, Flyway, and server settings
- `application-dev.yml` (new) — dev profile pointing at local Docker Compose Postgres

#### Decisions that matter

**1. `ddl-auto: validate`, not `update` or `create-drop`:**
Flyway owns the schema — it runs versioned SQL migration scripts in order. Hibernate's `validate` mode only checks that entity class fields match the actual DB columns, and crashes if they don't. This catches mapping mistakes at startup instead of silently ignoring them. `update` would let Hibernate modify the schema behind Flyway's back, creating drift between what Flyway thinks the schema is and what it actually is.

**2. `open-in-view: false`:**
Spring Boot's default is `true`, which keeps a Hibernate session open for the entire HTTP request — including while rendering the response. This means lazy-loaded relationships magically work in controller/view code, but it masks N+1 query bugs (each lazy access fires a separate SQL query you don't see) and holds database connections longer than necessary. Disabling it forces you to explicitly load the data you need in the service layer, which is noisy at first but prevents a whole class of production performance problems.

**3. Dev profile credentials match `docker-compose.yml` defaults:**
`nexus` / `nexus_local` are the same defaults used in the `${POSTGRES_PASSWORD:-nexus_local}` syntax from docker-compose.yml. This means `docker compose up -d` + `mvn spring-boot:run -Dspring.profiles.active=dev` works without creating a `.env` file. These are local-only defaults — they never reach production.

**4. `show-sql: true` + `format_sql: true` in dev only:**
Every SQL statement Hibernate generates is printed to the console. This is essential for catching the exact queries JPA produces — especially N+1 patterns where loading 100 tickets generates 101 queries instead of 2. In prod, this would flood logs and hurt performance.

**5. `clean-disabled: false` in dev only:**
Flyway's `clean` command drops the entire schema and re-runs all migrations from scratch. In dev, this is a lifesaver when you make a mistake in a migration. In prod, it would delete all data — which is why it's disabled by default and only enabled here.

#### What could go wrong
The dev credentials (`nexus` / `nexus_local`) are hardcoded in the profile file, not pulled from environment variables. This is fine for local dev (they match docker-compose.yml's defaults), but if someone changes the docker-compose.yml password without updating the dev profile, the app crashes with an authentication error on startup. The error message from Postgres is clear though — `FATAL: password authentication failed for user "nexus"` — so it's easy to diagnose.

---

### Unit 7: ArchUnit Domain-Purity Test

#### Before code — what and why
The domain layer (any `*.domain.*` package) must be pure Java — no Spring, no JPA, no Hibernate imports. This constraint is what keeps domain logic fast to test (no Spring context needed) and framework-independent. ArchUnit lets you write this constraint as a JUnit test: "no classes in `..domain..` should depend on classes in `org.springframework..` or `jakarta.persistence..`." The playbook (§2.3) says to add this in Phase 0, before any domain classes exist — it's cheapest to enforce a rule when nobody can violate it yet.

#### Files created
- `nexus-app/src/test/java/com/nexus/architecture/DomainPurityTest.java` — two ArchUnit tests (one for Spring imports, one for JPA/Hibernate imports)

#### Decisions that matter

**1. Two tests, not one:**
Splitting "no Spring" and "no JPA" into separate test methods gives clearer failure messages. If someone adds `@Entity` to a domain class, the failure says exactly "domain must not import JPA" — not a combined error mixing Spring and JPA violations.

**2. `allowEmptyShould(true)` — a real bug we hit and fixed:**
ArchUnit 1.3 defaults `failOnEmptyShould` to `true`. This means a rule that matches zero classes *fails* instead of passing trivially. Since no `*.domain.*` packages exist yet, both tests failed on the first run with: "Rule failed to check any classes." The fix is `.allowEmptyShould(true)` — the rule passes trivially now but will actively enforce once domain classes appear in Phase 1.

**Class of bug:** configuration-default mismatch. The library's default behavior changed between versions (older ArchUnit silently passed on empty rule sets), and the new default is stricter. Reading the error message told us exactly what to do.

**3. `ImportOption.DO_NOT_INCLUDE_TESTS`:**
Without this, ArchUnit would scan test classes too. Test helpers sometimes legitimately import Spring (`@SpringBootTest`), so we exclude them — the rule only applies to production code.

**4. `..domain..` double-dot syntax:**
In ArchUnit, `..` means "any sub-package at any depth." So `..domain..` matches `com.nexus.ticket.domain`, `com.nexus.ticket.domain.model`, `com.nexus.tenant.domain` — every domain package across every feature module.

#### What could go wrong
If someone creates a package named `domain` outside the expected structure (e.g., `com.nexus.shared.domain`), the rule catches it — which is correct, but might surprise someone who didn't expect `shared` to have a domain package. The fix is renaming the package, not weakening the rule.

---

### Unit 8: GitHub Actions CI Skeleton

#### Before code — what and why
If tests only run on a developer's laptop, the `main` branch will eventually break. Continuous Integration (CI) solves this by running a script automatically on every push and Pull Request. We use GitHub Actions, and per playbook §6, our pipeline isn't just running tests — it includes security gates that stop bad code *before* testing. We build this skeleton in Phase 0 so that from day one, every commit is checked for leaked secrets, unresolved stubs, and domain-purity violations.

#### Files created
- `.github/workflows/ci.yml` — the GitHub Actions workflow definition

#### Decisions that matter

**1. `gitleaks-action` for Secret Scanning:**
We run a Gitleaks step to catch accidentally committed `.env` files or API keys. If someone commits a hardcoded database password, this job fails immediately. It acts as the automated backstop to the "never commit secrets" rule.

**2. The `STUB:` grep check:**
The playbook allows using `// STUB: reason` to defer implementation, but forbids merging them to `main`. The shell command `grep -rnw . -e "STUB:"` scans the entire repository for the word "STUB:". If it finds it, it returns an exit code of 1, failing the build. This enforces the rule without human vigilance.

**3. Ordering of steps:**
We put linting, secret scanning, and STUB checking *before* the Java/Maven setup. Why? Because they are cheap and fast. If you committed a secret, we want the build to fail in 5 seconds, not after downloading Maven dependencies for 2 minutes. Fail fast saves time and compute.

**4. `mvn test` without wrappers:**
Since we didn't add the `mvnw` wrapper in previous units, the runner uses its globally installed Maven environment (via `actions/setup-java`). The `--no-transfer-progress` flag stops Maven from printing hundreds of lines of "Downloading..." to the CI logs, keeping the output readable.

#### What could go wrong
The `grep` command searches the entire directory. If you add a markdown file (like this journal!) that contains the word "STUB:" as part of an explanation, the CI will fail because it matched the text in the documentation. We will likely need to refine the `grep` later to only search `.java` files (e.g., using `--include=\*.java`) if this becomes a nuisance.

---
---

## Phase 1 — Domain Model, Migrations, RLS

### Unit 1: Domain Value Objects (Ticket Enums)

#### Before code — what and why
Before we can write a database table or a JPA entity, we need to define the vocabulary of the domain in pure Java. What statuses can a ticket have? What priorities? What categories? These enums are the building blocks everything else depends on — the Flyway migration will create columns matching these values, the REST API will validate against them, and the AI triage will assign them. They live in `com.nexus.ticket.domain` with zero framework imports, enforced by the ArchUnit test from Phase 0.

#### Files created
- `nexus-app/src/main/java/com/nexus/ticket/domain/TicketStatus.java` — the full lifecycle: NEW → CLASSIFIED → AI_DRAFTED → AUTO_RESOLVED | ESCALATED → IN_PROGRESS → RESOLVED → CLOSED
- `nexus-app/src/main/java/com/nexus/ticket/domain/TicketPriority.java` — LOW, MEDIUM, HIGH, CRITICAL
- `nexus-app/src/main/java/com/nexus/ticket/domain/TicketCategory.java` — BILLING, TECHNICAL, ACCOUNT, FEATURE_REQUEST, GENERAL

#### Decisions that matter

**1. Enums, not strings:**
Storing ticket status as a string ("new", "classified") means a typo like "classifed" compiles fine and only fails at runtime — or worse, silently creates a new status nobody expected. Enums catch this at compile time. The database will store these as VARCHAR via `@Enumerated(EnumType.STRING)` in the JPA entity later, so the DB column is human-readable in raw queries.

**2. Status enum names the states, doesn't enforce transitions:**
`TicketStatus` only defines *what states exist*. The rule "a CLOSED ticket can't go back to NEW" will be enforced by a separate state machine class — not by the enum itself. This is the State pattern: the enum is the vocabulary, the state machine is the grammar.

**3. Categories map to knowledge-base partitions:**
When the AI triage classifies a ticket as `TECHNICAL`, the RAG pipeline searches the `TECHNICAL` partition of the tenant's knowledge base first. This improves retrieval relevance — a billing question shouldn't match technical documentation.

**4. This is the first code in `com.nexus.ticket.domain`:**
This creates the package structure the master spec (§3) requires: feature modules as top-level packages (`ticket`, `tenant`, `ai`), with `domain/application/infrastructure/api` nested inside each. The ArchUnit test now scans real domain classes, not just an empty package.

#### What could go wrong
If we add a new enum value later (e.g., `WAITING_ON_CUSTOMER`), the Flyway migration that creates the column won't know about it — we'd need a new migration to add it to any CHECK constraint, or use `EnumType.STRING` (which we will) so Postgres stores the raw string and accepts new values without a schema change.

---

### Unit 2: Flyway Baseline Migration (`V1__baseline_schema.sql`)

#### Before code — what and why
We have domain enums but no database tables. This unit creates the first Flyway migration — a versioned SQL script that builds the `tenants` and `tickets` tables. Flyway tracks which migrations have been applied, so a fresh database gets the exact same schema as everyone else's. We set `ddl-auto: validate` in Phase 0 for this reason — Hibernate never touches the schema, only Flyway does.

#### Files created
- `nexus-app/src/main/resources/db/migration/V1__baseline_schema.sql` — creates `tenants`, `tickets`, indexes, and enables pgvector extension

#### Decisions that matter

**1. `TIMESTAMPTZ`, not `TIMESTAMP`:**
`TIMESTAMP` stores a date without timezone — if your server moves to a different timezone, every timestamp shifts. `TIMESTAMPTZ` stores an absolute point in time (internally always UTC), and Postgres converts to the session's timezone on display. For a multi-tenant SaaS with tenants in different timezones, this is non-negotiable.

**2. `gen_random_uuid()` as default, not application-generated:**
The database generates UUIDs, not Java. This means even raw SQL inserts get valid IDs, and there's no coordination needed between application instances. `gen_random_uuid()` is built into Postgres 13+, no extension needed.

**3. `VARCHAR(50)` for enum columns, not Postgres `ENUM` type:**
Postgres has a native `ENUM` type, but adding a new value to it requires `ALTER TYPE ... ADD VALUE` — which can't run inside a transaction. Using `VARCHAR` and storing the Java enum name as a string (`EnumType.STRING`) means adding a new enum value in Java is a code-only change, no migration needed.

**4. Two indexes on `tenant_id`:**
Every query in a multi-tenant system filters by `tenant_id`. Without an index, that's a full table scan on every request. The composite index `(tenant_id, status)` covers the most common dashboard query: "this tenant's tickets, filtered by status."

**5. `ON DELETE CASCADE` on `tenant_id` FK:**
If a tenant is deleted, all their tickets are deleted automatically. This prevents orphaned rows and simplifies tenant offboarding. In production you'd likely soft-delete tenants instead, but the FK constraint ensures referential integrity either way.

**6. `CREATE EXTENSION IF NOT EXISTS vector`:**
Enables pgvector now (Phase 0 uses the `pgvector/pgvector:pg16` image). We won't use embeddings until Phase 4, but enabling the extension in the baseline is free and means Phase 4's migration doesn't need to worry about it.

#### Verification
- Ran `mvn spring-boot:run -Dspring-boot.run.profiles=dev` — Flyway output: `Successfully applied 1 migration to schema "public", now at version v1`
- Verified via `psql`: `tenants`, `tickets`, and `flyway_schema_history` tables exist with correct columns and indexes

#### What could go wrong
If someone edits this file after it's been applied, Flyway will detect the checksum mismatch and refuse to start the app — "Migration checksum mismatch." The fix is never editing an applied migration; write a new `V2__` instead. This is Flyway working correctly, not a bug.

---
---

### Unit 3: JPA Entities (Infrastructure Layer)

#### Before code — what and why
We have domain enums (pure Java, no imports) and database tables (Flyway). Now we need the bridge: JPA entities — Java classes with `@Entity`, `@Table`, `@Column` that tell Hibernate how to map objects to rows. These live in `infrastructure.persistence`, NOT `domain`, because they carry `jakarta.persistence` imports. The dependency direction is infrastructure → domain (never the reverse). Since `ddl-auto: validate`, Hibernate checks these entities against the real schema at startup — any mismatch crashes the app immediately.

#### Files created
- `com.nexus.tenant.domain.PlanTier` — pure Java enum: FREE, STARTER, PROFESSIONAL, ENTERPRISE
- `com.nexus.tenant.infrastructure.persistence.TenantEntity` — JPA entity → `tenants` table
- `com.nexus.ticket.infrastructure.persistence.TicketEntity` — JPA entity → `tickets` table

#### Decisions that matter

**1. Infrastructure, not domain:**
`@Entity` is a JPA annotation — it tells Hibernate "this class maps to a database table." That's a framework concern, not a business logic concern. The domain enums (`TicketStatus`, `PlanTier`) are imported BY the entity, but the domain never imports the entity. This is the Clean Architecture dependency rule: dependencies point inward.

**2. `@Enumerated(EnumType.STRING)`, not `ORDINAL`:**
`ORDINAL` stores the enum's position number (0, 1, 2...). If someone reorders the enum values, every row in the database silently means something different. `STRING` stores the actual name ("NEW", "CLASSIFIED"), so reordering is safe and raw SQL queries are human-readable.

**3. `@Version` for optimistic locking:**
The `version` field on `TicketEntity` enables optimistic locking. When Hibernate updates a ticket, it adds `WHERE version = ?` to the UPDATE statement. If two agents edit the same ticket simultaneously, the second one's WHERE clause fails (the version was already incremented), and Hibernate throws `OptimisticLockException`. This prevents silent overwrites without heavyweight row locks.

**4. `FetchType.LAZY` on the tenant relationship:**
Loading a ticket doesn't automatically load the full tenant object. Without LAZY, querying 100 tickets would fire 100 extra SQL queries to load their tenants (the N+1 problem). With LAZY, Hibernate only loads the tenant when you explicitly access it. The `getTenantId()` helper method extracts just the FK value without triggering a query.

**5. No `@CreationTimestamp`/`@UpdateTimestamp`:**
We set timestamps in the constructor and setters instead of using Hibernate's auto-timestamp annotations. This keeps the behavior explicit — you can see exactly when `updatedAt` gets set. The database also has `DEFAULT now()` as a fallback for raw SQL inserts.

#### Verification
- App booted with `dev` profile: Hibernate validated both entities against the schema — no errors
- ArchUnit: 2/2 passed — `PlanTier` in `tenant.domain` has zero framework imports

---
---

### Unit 4: RLS Policies + Low-Privilege App Role

#### Before code — what and why
Without RLS, a bug in the application code (forgetting a `WHERE tenant_id = ?`) leaks every tenant's data. RLS moves this protection into the database: Postgres automatically filters rows by `tenant_id`, even for queries that forgot to include the filter. Three things were needed: (1) a low-privilege `nexus_app` role (separate from the Flyway owner), (2) RLS policies on `tickets`, (3) `FORCE ROW LEVEL SECURITY` as a safety net.

#### Files created/modified
- `nexus-app/src/main/resources/db/migration/V2__rls_policies.sql` — creates `nexus_app` role, enables RLS, creates tenant isolation policy
- `nexus-app/src/main/resources/application-dev.yml` — split datasource: app uses `nexus_app`, Flyway uses `nexus`
- `docker-compose.yml` — all host ports moved to high range (15432, 16379, 19092) to permanently avoid Windows Hyper-V port conflicts

#### Decisions that matter

**1. Two separate database roles:**
`nexus` (owner) runs Flyway migrations — it can CREATE/ALTER/DROP tables. `nexus_app` (runtime) runs the app — it can only SELECT/INSERT/UPDATE/DELETE. This is the principle of least privilege. If the app is compromised, the attacker can't drop tables.

**2. `SET LOCAL app.tenant_id` for tenant context:**
`SET LOCAL` sets a transaction-scoped variable. It resets automatically when the transaction ends. This is critical for connection pooling: if the app uses HikariCP and reuses connections across requests, `SET LOCAL` ensures one tenant's context never leaks into another tenant's request.

**3. `current_setting('app.tenant_id', true)` — the `true` parameter:**
If the setting hasn't been set, `current_setting()` normally throws an error. The `true` parameter makes it return NULL instead. Since `tenant_id = NULL` is always false in SQL (NULL is not equal to anything), this means "if you forget to set tenant context, you see nothing." This is fail-closed — the safe default.

**4. `WITH CHECK` on the policy:**
`USING` filters rows on SELECT/UPDATE/DELETE. `WITH CHECK` validates rows on INSERT/UPDATE — it prevents a tenant from inserting a ticket with a different `tenant_id`. Both clauses use the same expression, ensuring no cross-tenant writes.

**5. `FORCE ROW LEVEL SECURITY`:**
By default, RLS doesn't apply to the table owner. `FORCE` makes it apply even to the owner. However, Postgres superusers always bypass RLS — this is a Postgres design decision. The `nexus` role in Docker Compose is a superuser, so it sees all rows regardless. The `nexus_app` role is NOT a superuser, so RLS works correctly for the app.

**6. All Docker ports moved to high range:**
Windows Hyper-V dynamically reserves port ranges that change on every reboot. Standard ports (5432, 6379, 9092) randomly fall into reserved ranges. Ports 15432, 16379, 19092 are high enough to avoid this permanently.

#### Verification (RLS gate tests)
1. `nexus_app` with no context → 0 rows (fail-closed) ✅
2. `nexus_app` with Acme tenant → only Acme's tickets ✅
3. `nexus_app` with Beta tenant → only Beta's ticket ✅
4. `nexus` (superuser/owner) → all tickets visible (expected — superusers bypass RLS) ✅

---
---

### Unit 5: Ticket State Machine (Domain)

#### Before code — what and why
The `TicketStatus` enum from Unit 1 names the states but doesn't prevent illegal transitions (e.g., CLOSED → NEW). The state machine enforces the grammar: which transitions are legal. It's pure Java in the domain package — tested in milliseconds without Spring.

#### Files created
- `com.nexus.ticket.domain.TicketStateMachine` — static utility with `canTransition()`, `transition()`, `allowedTransitions()`, `isTerminal()`
- `com.nexus.ticket.domain.TicketStateMachineTest` — 22 tests covering legal transitions, illegal transitions, terminal states

#### Decisions that matter

**1. Static methods, not instance:**
The state machine has no mutable state — the transition table is a `static final` map. Making it a utility class with static methods means no wiring, no Spring bean, no lifecycle management. You just call `TicketStateMachine.canTransition(from, to)`.

**2. `EnumMap` and `EnumSet`:**
These are Java's special collections for enum keys/values. They use arrays internally instead of hash buckets, so lookups are O(1) with minimal memory overhead. For a state machine that's checked on every ticket update, this matters.

**3. `transition()` throws `IllegalStateException`:**
If a transition is illegal, the method throws with a descriptive message including the current state and what transitions ARE allowed. This gives clear errors in logs: "Illegal ticket transition: CLOSED → NEW. Allowed from CLOSED: []". The service layer will catch this and return a 400 Bad Request.

**4. Test speed proves the architecture:**
22 tests ran in 0.258s total — no Spring boot, no database. If the state machine imported JPA or Spring, these tests would need `@SpringBootTest` and take 5+ seconds. This is the concrete payoff of the domain purity rule.

#### Verification
- 22/22 tests pass (8 legal transitions, 9 illegal transitions, 2 terminal states, 1 query, 1 exception message, 1 ArchUnit)
- ArchUnit: 2/2 pass — `TicketStateMachine` has zero framework imports

---

### Unit 6: Phase 1 Gate Verification & Closure

#### What was verified
- Full test suite: 22/22 pass (ArchUnit 2, state machine 20)
- RLS manual tests: 4/4 pass (no-context=0 rows, Acme=2, Beta=1, owner=all)
- Flyway V1+V2 applied cleanly, Hibernate validate passes
- App boots on dev profile with split datasource (nexus_app for app, nexus for Flyway)
- CURRENT_STATE.md updated with gate evidence

#### Phase 1 summary — what was built
1. **Domain vocabulary:** `TicketStatus` (8 states), `TicketPriority` (4 levels), `TicketCategory` (5 types), `PlanTier` (4 tiers) — all pure Java enums
2. **Flyway migrations:** V1 (tenants + tickets tables), V2 (nexus_app role + RLS policies)
3. **JPA entities:** `TenantEntity`, `TicketEntity` in infrastructure.persistence — bridge between domain and database
4. **State machine:** `TicketStateMachine` — enforces legal transitions, tested with 20 domain tests in <0.3s
5. **RLS:** `tenant_isolation` policy on tickets, fail-closed, `FORCE ROW LEVEL SECURITY`
6. **Role separation:** `nexus` (owner/Flyway) vs `nexus_app` (runtime, RLS-filtered)

#### Infrastructure fix
All Docker Compose host ports moved to high range (15432/16379/19092) to permanently avoid Windows Hyper-V/WinNAT dynamic port reservation conflicts.

---

## Phase 2 — Core REST API: DTOs, Validation, Exception Handling, Tenant Context, CRUD

### Unit 1: Tenant Context Filter (the RLS bridge)

#### What was built
- `TenantContext.java` — ThreadLocal holder for current tenant ID (`set`, `get`, `clear`)
- `TenantContextFilter.java` — Servlet filter that extracts tenantId from URL path (`/api/v1/tenants/{tenantId}/...`), validates as UUID, stores in ThreadLocal
- `TenantAwareDataSource.java` — DataSource wrapper using Java `Proxy` to intercept `setAutoCommit(false)` and inject `SET LOCAL app.tenant_id` into every transaction
- `TenantDataSourceConfig.java` — BeanPostProcessor that wraps HikariCP DataSource with the tenant-aware version

#### Why this design
- `SET LOCAL` must run **inside** a transaction (not before it) — Postgres resets it on commit/rollback, so pooled connections never leak tenant context
- `SET` (without LOCAL) would be session-scoped and **leak across requests** through HikariCP's connection pool — a real cross-tenant data breach
- Java `Proxy` avoids implementing all ~60 Connection interface methods — only `setAutoCommit` is intercepted
- UUID validation before string interpolation in `SET LOCAL` prevents SQL injection
- Filter's `finally` block always calls `TenantContext.clear()` to prevent ThreadLocal leakage when Tomcat reuses threads

#### The request flow
```
HTTP request → TenantContextFilter sets ThreadLocal
  → @Transactional starts → setAutoCommit(false) intercepted
    → SET LOCAL app.tenant_id = '...' executed
      → Postgres RLS filters all queries automatically
    → Transaction commits → SET LOCAL resets
  → Filter finally block → TenantContext.clear()
```

#### Package placement
All four classes live in `com.nexus.common.multitenancy` — cross-cutting infrastructure, not domain or ticket-specific.

#### Test results
22/22 existing tests pass (ArchUnit 2, state machine 20). No new tests needed for Unit 1 — the filter and DataSource wrapper will be exercised by integration tests in later units.

---

### Unit 2: DTOs + Validation + Mapper

#### What was built
- `CreateTicketRequest.java` — inbound DTO, `@NotBlank` on subject, `@Size` limits
- `UpdateTicketRequest.java` — inbound DTO, all fields optional (partial updates)
- `TransitionTicketRequest.java` — inbound DTO for state transitions (separated from update because transitions go through state machine validation)
- `TicketResponse.java` — outbound DTO (the only shape serialized to JSON)
- `TicketMapper.java` — manual entity↔DTO conversion, static utility methods

#### Why Java records
All DTOs are `record` types — immutable, auto-generate `equals`/`hashCode`/`toString`, and have no boilerplate. Perfect for data carriers that don't need setters.

#### Why manual mapper (not MapStruct)
MapStruct is a compile-time code generator that auto-generates mapping code. We use manual mapping because: (a) transparent — you can step through it in the debugger, (b) no annotation-processor dependency, (c) our entity→DTO mapping is simple enough that auto-generation adds complexity without benefit.

#### Package placement
All DTOs in `com.nexus.ticket.application.dto` — the `application` layer sits between `domain` (pure Java) and `infrastructure` (JPA/Spring). DTOs live here because they're the API contract, not domain logic.

#### Test results
22/22 existing tests pass.

---

### Unit 3: Custom Exceptions + Global Exception Handler

#### What was built
- `ErrorResponse.java` — standard JSON error shape (`status`, `error`, `message`, `timestamp`)
- `TenantNotFoundException.java` — tenant ID not in DB → HTTP 404
- `TicketNotFoundException.java` — ticket not found (or hidden by RLS) → HTTP 404
- `IllegalTicketTransitionException.java` — state machine violation → HTTP 409 Conflict
- `GlobalExceptionHandler.java` — `@RestControllerAdvice` mapping each exception to correct HTTP status

#### Exception → HTTP status mapping
| Exception | HTTP Status | When |
|---|---|---|
| `MethodArgumentNotValidException` | 400 Bad Request | Bean Validation fails (`@NotBlank`, `@Size`) |
| `IllegalArgumentException` | 400 Bad Request | Invalid enum value (e.g., `category=INVALID`) |
| `TenantNotFoundException` | 404 Not Found | Tenant ID not in database |
| `TicketNotFoundException` | 404 Not Found | Ticket not found or hidden by RLS |
| `IllegalTicketTransitionException` | 409 Conflict | State machine rejects transition |
| `ObjectOptimisticLockingFailureException` | 409 Conflict | Concurrent edit on same ticket |
| `Exception` (catch-all) | 500 Internal Server Error | Anything unexpected |

#### Why 409 for transitions (not 400)
400 = "your request is malformed" (bad syntax). 409 = "your request is valid but conflicts with the current state of the resource." An illegal transition is valid JSON with a real status name — it just can't be applied to *this* ticket in *this* state.

#### Test results
22/22 existing tests pass.

---

### Unit 4: Repository Layer + Specifications

#### What was built
- `TenantRepository.java` — `JpaRepository<TenantEntity, UUID>` for tenant existence checks
- `TicketRepository.java` — `JpaRepository<TicketEntity, UUID>` + `JpaSpecificationExecutor` for dynamic filtering
- `TicketSpecifications.java` — composable `Specification` lambdas for status, priority, category filters

#### Why JpaSpecificationExecutor
The ticket list endpoint supports optional filters (status, priority, category). With 3 optional filters, that's 8 possible combinations. Instead of writing 8 query methods, Specifications compose dynamically:
```java
Specification<TicketEntity> spec = Specification.where(null);
if (status != null) spec = spec.and(hasStatus(status));
if (priority != null) spec = spec.and(hasPriority(priority));
repository.findAll(spec, pageable); // builds the WHERE clause at runtime
```

#### Why no WHERE tenant_id = ? in any query
RLS handles tenant filtering at the Postgres level. Every query from `nexus_app` automatically has `WHERE tenant_id = current_setting('app.tenant_id')` appended by Postgres. The repository doesn't know about tenants at all.

#### Test results
22/22 existing tests pass. New integration tests for repositories come in Unit 6.

---

### Unit 5: Service Layer + Unit Tests

#### What was built
- `TicketService.java` — business logic: create, get, list (with filters + pagination), update (partial), transition (via state machine), delete
- `TicketServiceTest.java` — 12 Mockito-based unit tests covering happy paths and error cases for all 5 operations

#### How @Transactional triggers RLS
Every service method is `@Transactional`. When Spring starts the transaction, it calls `setAutoCommit(false)` on the connection. Our `TenantAwareDataSource` intercepts this and runs `SET LOCAL app.tenant_id = '...'`. From that point, every query on that connection is filtered by RLS automatically. This is why the service code never mentions `tenant_id` in any query.

#### State machine integration
`transitionTicket()` calls `TicketStateMachine.transition(from, to)` from Phase 1. If the state machine throws `IllegalStateException`, the service catches it and throws `IllegalTicketTransitionException` instead — which the `GlobalExceptionHandler` maps to HTTP 409.

#### Test approach
- **Mockito** mocks repositories — no database needed, tests run in <2s
- **@ExtendWith(MockitoExtension.class)** — no Spring context needed
- Nested test classes group by operation: `CreateTicket`, `GetTicket`, `UpdateTicket`, `TransitionTicket`, `DeleteTicket`
- Each has a happy path + at least one error case

#### Test results
34/34 total (22 existing + 12 new).

---

*This document is updated every unit. Scroll to the bottom for the latest.*

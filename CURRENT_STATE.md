# Current State

**Phase:** 1 — Domain model, migrations, RLS (COMPLETE, pending human sign-off)
**Last passing gate (automated evidence):** Phase 1 — 22/22 tests pass (2026-07-17), RLS 4/4 manual tests pass (2026-07-17)
**Human manual sign-off (§2.10):** Phase 0 — confirmed by human (2026-07-12)
**Last passing test run:** `mvn test -pl nexus-app` — 22 tests, 0 failures (2026-07-17)

## Open questions blocked on a human answer
- None

## Phase 1 gate evidence
1. **Domain purity:** ArchUnit 2/2 pass — all domain classes have zero framework imports
2. **State machine:** 20 domain tests pass in <0.3s (no Spring context)
3. **Flyway migrations:** V1 (schema) + V2 (RLS) applied cleanly
4. **Hibernate validate:** App boots without schema mismatch errors
5. **RLS verified:**
   - `nexus_app` with no context → 0 rows (fail-closed) ✅
   - `nexus_app` with Acme context → only Acme's tickets ✅
   - `nexus_app` with Beta context → only Beta's ticket ✅
   - `nexus` (superuser) → all rows visible (expected) ✅

## In progress right now
- Phase 1 complete (6 units on `feat/domain-model-rls` branch)
- Next: Phase 2 — REST API, Tenant Context Filter, CRUD endpoints

## Known deviations from the playbook (should be rare, must have an ADR)
- Docker Compose host ports changed from standard (5432/6379/9092) to high range (15432/16379/19092) due to Windows Hyper-V dynamic port reservation conflicts

## Reference docs present in this repo (checked at session start, §1)
- `/docs/nexus-master-spec.md`: present
- `/docs/nexus-architecture-rationale.md`: present
- `/docs/nexus-agent-guardrails.md`: present

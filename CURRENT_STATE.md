# Current State

**Phase:** 0 — Repo scaffold
**Last passing gate (automated evidence):** none yet
**Human manual sign-off (§2.10):** none yet
**Last passing test run:** `mvn test -pl nexus-app -Dtest="com.nexus.architecture.DomainPurityTest"` (2026-07-08)

## Open questions blocked on a human answer
- None at kickoff — the four pre-flight questions are resolved, see playbook §9. Add new ones here only as they genuinely arise.

## In progress right now
- Phase 1, Unit 1 (next): Domain model skeleton and Flyway baseline
- Units 1–8 complete: CURRENT_STATE.md, SETUP_CHECKLIST.md, parent POM, child POM, NexusApplication.java, application.yml, Docker Compose, .env.example, Spring Profiles, ArchUnit domain-purity test, GitHub Actions CI
- Branch: `feat/repo-scaffold`

## Known deviations from the playbook (should be rare, must have an ADR)
- none

## Reference docs present in this repo (checked at session start, §1)
- `/docs/nexus-master-spec.md`: present
- `/docs/nexus-architecture-rationale.md`: present
- `/docs/nexus-agent-guardrails.md`: present

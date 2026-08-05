# Nexus

[![CI/CD Pipeline](https://github.com/mavericaks/Nexus/actions/workflows/ci.yml/badge.svg)](https://github.com/mavericaks/Nexus/actions/workflows/ci.yml)

Multi-tenant AI-powered customer support SaaS — Spring Boot + Spring AI, built as a portfolio project demonstrating production-grade architecture end to end.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **API** | Spring Boot 3.4, Spring MVC, Spring Security (JWT + OAuth2) |
| **AI** | Spring AI → Groq (Llama 3.3 70B), Google Gemini Embeddings, RAG |
| **Database** | PostgreSQL 16 + pgvector, Flyway migrations, Row-Level Security |
| **Messaging** | Apache Kafka (KRaft mode) |
| **Caching** | Redis (Upstash in production) |
| **Resilience** | Resilience4j (Circuit Breaker + Retry) |
| **Observability** | Micrometer + Prometheus + Grafana, Structured JSON Logging |
| **Testing** | JUnit 5, Testcontainers, Mockito, JaCoCo |
| **CI/CD** | GitHub Actions (8-step pipeline), Docker, Render |

## Quick Start (Local Development)

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Create .env from template
cp .env.example .env
# Fill in your API keys (GROQ_API_KEY, GEMINI_API_KEY, etc.)

# 3. Run the application
./mvnw spring-boot:run -pl nexus-app -Dspring-boot.run.profiles=dev

# 4. Run tests
./mvnw verify
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/login` | JWT login |
| `GET` | `/api/v1/tenants/{id}/tickets` | List tickets (paginated, filtered) |
| `POST` | `/api/v1/tenants/{id}/tickets` | Create ticket |
| `POST` | `/api/v1/tenants/{id}/tickets/{id}/triage` | Trigger AI triage |
| `PUT` | `/api/v1/tenants/{id}/tickets/{id}/transition` | Transition ticket status |

## Architecture

- **Multi-tenant:** Row-Level Security (RLS) in Postgres, per-tenant isolation at the database level
- **Domain-Driven:** Clean separation of domain, application, infrastructure, and API layers
- **AI-Powered:** LLM-based ticket classification with RAG over a knowledge base (pgvector)
- **Event-Driven:** Kafka for async ticket lifecycle events, Spring ApplicationEventPublisher for in-process fan-out
- **Resilient:** Circuit breaker + retry around LLM calls, per-tenant rate limiting

## Documentation

- [`NEXUS-AGENT-PLAYBOOK.md`](./NEXUS-AGENT-PLAYBOOK.md) — The operational playbook
- [`KNOWLEDGE-JOURNAL.md`](./KNOWLEDGE-JOURNAL.md) — Phase-by-phase build journal
- [`docs/`](./docs/) — Architecture rationale and master spec

## Deployment

Deployed to [Render](https://render.com) with Docker. See [`render.yaml`](./render.yaml) for the infrastructure blueprint.

```bash
# The CI/CD pipeline automatically deploys on push to main.
# Manual deploy:
docker build -t nexus-app .
docker run -p 8080:8080 --env-file .env nexus-app
```

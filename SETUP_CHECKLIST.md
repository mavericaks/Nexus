# Setup Checklist

| Credential/Account | Needed for phase | Status | .env variable | Notes |
|---|---|---|---|---|
| Neon Postgres connection string | 1 | pending | DATABASE_URL | |
| Neon low-privilege app role | 1 | pending | APP_DATABASE_URL | separate from Flyway role |
| OAuth provider — Google (decided, §9) | 3 | pending | OAUTH_GOOGLE_CLIENT_ID / OAUTH_GOOGLE_CLIENT_SECRET | human creates in Google Cloud Console |
| JWT signing secret (agent-generates, §9) | 3 | pending | JWT_SECRET | agent runs `openssl rand -base64 32` as a visible command at Phase 3 start |
| GROQ_API_KEY | 4 | pending | GROQ_API_KEY | |
| Embeddings — Google Gemini Embedding API (decided, §9) | 4 | pending | GEMINI_API_KEY | separate credential from OAuth above — from AI Studio, not Cloud Console; write the ADR at Phase 4 start |
| Backblaze B2 key | 4 (if KB upload) | pending | B2_KEY_ID / B2_APP_KEY | |
| Upstash Redis | 6 | pending | REDIS_URL / REDIS_TOKEN | |
| Sentry DSN | 7 | pending | SENTRY_DSN | optional |
| Render account/service + GH secrets | 9 | pending | (GitHub repo secrets, not .env) | human adds via GitHub UI |
| Kubeadm cluster — build fresh (decided, §9) | 10 | pending | N/A | not a credential — a build task on the current laptop, see §5 Phase 10 sizing table |

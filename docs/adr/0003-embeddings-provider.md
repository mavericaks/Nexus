# ADR 0003: Embeddings Provider — Google Gemini Embedding API

## Status
Accepted

## Context
The RAG (Retrieval-Augmented Generation) pipeline in Phase 4 requires an embeddings provider to convert text (knowledge base articles, ticket queries) into dense vectors for similarity search via pgvector.

**Problem:** Groq, our LLM inference provider, does not offer an embeddings API endpoint. Its catalog covers chat/completion, reasoning, and Whisper speech-to-text only. We need a separate provider for embeddings.

**Options evaluated:**

| Option | Cost | Latency | Quality | Complexity |
|--------|------|---------|---------|------------|
| **Google Gemini Embedding API** (`text-embedding-004`) | Free (AI Studio, 1500 RPM) | ~100ms | High (768d vectors, state-of-the-art) | Low — Spring AI has native `spring-ai-starter-model-google-genai-embedding` |
| Local `sentence-transformers` (e.g., `all-MiniLM-L6-v2`) | Free | ~50ms (CPU) | Good (384d vectors) | Medium — needs Python/ONNX runtime, complicates Java-only stack |
| OpenAI Embeddings (`text-embedding-3-small`) | $0.02/1M tokens | ~200ms | High | Low — but adds a paid dependency |

## Decision
Use **Google Gemini Embedding API** (`text-embedding-004`) via `spring-ai-starter-model-google-genai-embedding`.

## Rationale
1. **Free tier is generous** — 1500 requests/minute on AI Studio, no credit card required.
2. **Spring AI native support** — auto-configured starter, no custom HTTP client code.
3. **High quality** — 768-dimensional vectors, top-tier retrieval performance.
4. **Same ecosystem** — we already use Google for OAuth2; one more Google API key is simpler than adding a Python sidecar.
5. **Port/adapter pattern** — the `EmbeddingService` interface abstracts the provider, so swapping to a local model or OpenAI later requires only a new implementation class.

## Consequences
- Adds `GEMINI_API_KEY` to the credential checklist (separate from the Google OAuth credentials — this comes from AI Studio, not Cloud Console).
- Embedding dimension is 768; the pgvector column must match (`vector(768)`).
- CI tests must NOT call real Gemini — use a mock/fake embedding service in tests (same port/adapter pattern as the LLM client, per guardrails §9.4).

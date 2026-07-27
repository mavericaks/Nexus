-- ============================================================
-- V4: Knowledge Base + pgvector for RAG
-- ============================================================
-- Creates the knowledge_articles table with a vector column for
-- similarity search. Each article belongs to a tenant (RLS-protected).
-- Seeded with sample articles so the triage agent has something
-- to search against immediately.
-- ============================================================

-- Enable pgvector extension (idempotent)
CREATE EXTENSION IF NOT EXISTS vector;

-- ─── Knowledge Articles Table ──────────────────────────────────
CREATE TABLE knowledge_articles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    title           VARCHAR(500) NOT NULL,
    content         TEXT NOT NULL,
    category        VARCHAR(50),
    -- 768-dimensional vector for Gemini text-embedding-004
    embedding       vector(768),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for fast vector similarity search (HNSW = Hierarchical
-- Navigable Small World — faster than IVFFlat for recall, slightly
-- more memory, but ideal for our scale)
CREATE INDEX idx_knowledge_articles_embedding
    ON knowledge_articles
    USING hnsw (embedding vector_cosine_ops);

-- Tenant FK index for RLS join performance
CREATE INDEX idx_knowledge_articles_tenant_id
    ON knowledge_articles(tenant_id);

-- ─── RLS — same pattern as tickets ─────────────────────────────
ALTER TABLE knowledge_articles ENABLE ROW LEVEL SECURITY;
ALTER TABLE knowledge_articles FORCE ROW LEVEL SECURITY;

-- App role can only see articles belonging to the current tenant
CREATE POLICY knowledge_articles_tenant_isolation
    ON knowledge_articles
    FOR ALL
    TO nexus_app
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Grant CRUD to app role (no DDL)
GRANT SELECT, INSERT, UPDATE, DELETE ON knowledge_articles TO nexus_app;

-- ─── Seed Knowledge Base Articles ──────────────────────────────
-- Acme Corp (tenant aaaa...) gets sample support KB articles.
-- Embeddings will be generated at app startup or via a seed script.
-- For now, embedding column is NULL — populated when the embedding
-- service runs.

INSERT INTO knowledge_articles (tenant_id, title, content, category) VALUES
-- Password reset
('aaaa0000-0000-0000-0000-000000000001',
 'How to Reset Your Password',
 'To reset your password, go to the login page and click "Forgot Password". Enter your email address and we will send you a password reset link. The link expires after 24 hours. If you don''t receive the email, check your spam folder. For security, you cannot reuse your last 5 passwords. If you''re still having trouble, contact support and we can manually trigger a password reset from the admin panel.',
 'ACCOUNT'),

-- Billing inquiry
('aaaa0000-0000-0000-0000-000000000001',
 'Understanding Your Invoice',
 'Invoices are generated on the 1st of each month and sent to the billing email on file. Each invoice includes: subscription fee, usage charges (if applicable), taxes, and any credits or adjustments. You can download past invoices from Settings > Billing > Invoice History. If you see an unexpected charge, it may be from a plan upgrade mid-cycle — we prorate upgrades. For refund requests, contact billing support within 30 days of the charge.',
 'BILLING'),

-- API integration
('aaaa0000-0000-0000-0000-000000000001',
 'Getting Started with the API',
 'Our REST API uses Bearer token authentication. Generate an API key from Settings > Developer > API Keys. All requests must include the header "Authorization: Bearer YOUR_API_KEY". Rate limits are 100 requests/minute for free plans and 1000/minute for paid plans. The base URL is https://api.example.com/v1. Full API documentation is available at https://docs.example.com/api. Common endpoints: GET /tickets (list), POST /tickets (create), PATCH /tickets/:id (update).',
 'TECHNICAL'),

-- Account setup
('aaaa0000-0000-0000-0000-000000000001',
 'Setting Up Two-Factor Authentication (2FA)',
 'Two-factor authentication adds an extra layer of security. Go to Settings > Security > Two-Factor Authentication and click Enable. You can use an authenticator app (Google Authenticator, Authy) or SMS verification. We recommend using an authenticator app as it''s more secure. After enabling 2FA, you''ll need to enter a verification code each time you log in. Save your backup codes somewhere safe — you''ll need them if you lose access to your authenticator.',
 'ACCOUNT'),

-- Product feature
('aaaa0000-0000-0000-0000-000000000001',
 'Using the Dashboard Analytics',
 'The analytics dashboard shows key metrics for your support operations. Available views: Overview (ticket volume, resolution time, satisfaction score), Agent Performance (tickets per agent, average response time), and Trends (weekly/monthly comparisons). You can filter by date range, category, priority, and agent. Export data as CSV from any view using the Export button. Custom reports are available on the Enterprise plan.',
 'GENERAL'),

-- Shipping/returns (different domain to test category classification)
('aaaa0000-0000-0000-0000-000000000001',
 'Return and Refund Policy',
 'We offer a 30-day return policy for all products. Items must be in original condition with tags attached. To initiate a return, go to Orders > select the order > Request Return. You''ll receive a prepaid shipping label via email. Refunds are processed within 5-7 business days after we receive the returned item. For defective items, we offer free expedited replacement shipping. Digital products and gift cards are non-refundable.',
 'BILLING');

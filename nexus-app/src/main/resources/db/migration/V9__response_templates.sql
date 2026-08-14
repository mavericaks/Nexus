-- V9__response_templates.sql
-- Pre-built response templates that agents can insert when replying to tickets.
-- Supports variable placeholders like {{customer_name}}, {{ticket_id}}.
-- RLS-protected: each tenant has their own template library.

CREATE TABLE response_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    category    VARCHAR(50),           -- Optional: BILLING, TECHNICAL, ACCOUNT, etc.
    created_by  UUID         REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_response_templates_tenant ON response_templates(tenant_id);

COMMENT ON TABLE response_templates IS 'Reusable response templates for agents. Supports {{variable}} placeholders.';

-- ─── RLS ────────────────────────────────────────────────────────────
ALTER TABLE response_templates ENABLE ROW LEVEL SECURITY;

CREATE POLICY response_templates_tenant_isolation ON response_templates
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

ALTER TABLE response_templates FORCE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON response_templates TO nexus_app;

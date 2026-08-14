-- V6__ticket_notes.sql
-- Internal notes on tickets for agent-to-agent collaboration.
-- These are never visible to end customers — only to authenticated agents.
-- RLS-protected: each tenant only sees their own notes.

CREATE TABLE ticket_notes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID         NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    author_id   UUID         NOT NULL REFERENCES users(id),
    author_name VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_notes_ticket ON ticket_notes(ticket_id);

COMMENT ON TABLE ticket_notes IS 'Internal agent notes on tickets. Never exposed to customers.';

-- ─── RLS ────────────────────────────────────────────────────────────
ALTER TABLE ticket_notes ENABLE ROW LEVEL SECURITY;

CREATE POLICY ticket_notes_tenant_isolation ON ticket_notes
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

ALTER TABLE ticket_notes FORCE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON ticket_notes TO nexus_app;

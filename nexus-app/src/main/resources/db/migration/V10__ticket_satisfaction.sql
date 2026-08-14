-- V10__ticket_satisfaction.sql
-- Customer satisfaction (CSAT) ratings for resolved tickets.
-- One rating per ticket (enforced by UNIQUE constraint on ticket_id).
-- RLS-protected: each tenant only sees their own satisfaction data.

CREATE TABLE ticket_satisfaction (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID         NOT NULL UNIQUE REFERENCES tickets(id) ON DELETE CASCADE,
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    score       INT          NOT NULL CHECK (score BETWEEN 1 AND 5),  -- 1=😡 2=😟 3=😐 4=🙂 5=😍
    feedback    TEXT,                                                  -- Optional one-liner
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_satisfaction_tenant ON ticket_satisfaction(tenant_id);

COMMENT ON TABLE ticket_satisfaction IS 'CSAT ratings. One per ticket. Scale: 1 (terrible) to 5 (excellent).';
COMMENT ON COLUMN ticket_satisfaction.score IS '1=terrible, 2=poor, 3=neutral, 4=good, 5=excellent.';

-- ─── RLS ────────────────────────────────────────────────────────────
ALTER TABLE ticket_satisfaction ENABLE ROW LEVEL SECURITY;

CREATE POLICY ticket_satisfaction_tenant_isolation ON ticket_satisfaction
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

ALTER TABLE ticket_satisfaction FORCE ROW LEVEL SECURITY;

GRANT SELECT, INSERT ON ticket_satisfaction TO nexus_app;

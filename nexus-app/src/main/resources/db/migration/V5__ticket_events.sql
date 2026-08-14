-- V5__ticket_events.sql
-- Activity timeline / audit trail for tickets.
-- Every mutation (create, status change, triage, field update) is recorded as an event.
-- RLS-protected: each tenant only sees their own events.

CREATE TABLE ticket_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID         NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    tenant_id   UUID         NOT NULL REFERENCES tenants(id),
    event_type  VARCHAR(50)  NOT NULL,  -- CREATED, STATUS_CHANGED, TRIAGED, FIELD_UPDATED, NOTE_ADDED, ASSIGNED, SLA_BREACHED
    actor_id    UUID,                   -- NULL for system-generated events
    actor_name  VARCHAR(255),
    details     JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_events_ticket ON ticket_events(ticket_id);
CREATE INDEX idx_ticket_events_tenant ON ticket_events(tenant_id);

COMMENT ON TABLE ticket_events IS 'Immutable audit trail. Each row is a single event in a ticket lifecycle.';
COMMENT ON COLUMN ticket_events.event_type IS 'CREATED, STATUS_CHANGED, TRIAGED, FIELD_UPDATED, NOTE_ADDED, ASSIGNED, SLA_BREACHED';
COMMENT ON COLUMN ticket_events.details IS 'Flexible JSONB payload, e.g. {"from":"NEW","to":"CLASSIFIED"} or {"field":"priority","old":"LOW","new":"HIGH"}';

-- ─── RLS ────────────────────────────────────────────────────────────
ALTER TABLE ticket_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY ticket_events_tenant_isolation ON ticket_events
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

ALTER TABLE ticket_events FORCE ROW LEVEL SECURITY;

GRANT SELECT, INSERT ON ticket_events TO nexus_app;

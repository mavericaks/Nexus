-- V7__notifications.sql
-- In-app notification queue for users.
-- Notifications are user-scoped (not tenant-scoped via RLS in the usual way).
-- We still include tenant_id for data integrity but RLS is on user_id.

CREATE TABLE notifications (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenants(id),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(50)  NOT NULL,  -- ESCALATION, SLA_BREACH, TRIAGE_COMPLETE, ASSIGNMENT, NOTE_ADDED
    title         VARCHAR(255) NOT NULL,
    message       TEXT,
    reference_id  UUID,                   -- ticket_id or other entity ID for deep-linking
    is_read       BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_tenant ON notifications(tenant_id);

COMMENT ON TABLE notifications IS 'In-app notification queue. Each row targets a specific user.';
COMMENT ON COLUMN notifications.type IS 'ESCALATION, SLA_BREACH, TRIAGE_COMPLETE, ASSIGNMENT, NOTE_ADDED';
COMMENT ON COLUMN notifications.reference_id IS 'FK to the related entity (e.g. ticket_id) for deep-linking in the UI.';

-- ─── RLS ────────────────────────────────────────────────────────────
-- Notifications are tenant-isolated AND user-scoped.
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY notifications_tenant_isolation ON notifications
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

ALTER TABLE notifications FORCE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE ON notifications TO nexus_app;

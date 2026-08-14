-- V8__user_preferences.sql
-- User-level preferences for theme, notification settings, dashboard layout.
-- Not RLS-scoped — keyed by user_id (one row per user).

CREATE TABLE user_preferences (
    user_id             UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_digest        VARCHAR(20)  NOT NULL DEFAULT 'OFF',  -- OFF, DAILY, WEEKLY
    notification_sound  BOOLEAN      NOT NULL DEFAULT true,
    theme               VARCHAR(20)  NOT NULL DEFAULT 'DARK', -- DARK, LIGHT, SYSTEM
    dashboard_layout    JSONB        NOT NULL DEFAULT '{}',   -- Widget positions and visibility
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE user_preferences IS 'Per-user UI/notification preferences. One row per user.';
COMMENT ON COLUMN user_preferences.email_digest IS 'OFF, DAILY, WEEKLY — controls email summary frequency.';
COMMENT ON COLUMN user_preferences.dashboard_layout IS 'JSON object storing widget visibility and order for customizable dashboard.';

-- No RLS needed — user_preferences is keyed by user_id.
-- The service layer filters by the authenticated user's ID from JWT.

GRANT SELECT, INSERT, UPDATE ON user_preferences TO nexus_app;

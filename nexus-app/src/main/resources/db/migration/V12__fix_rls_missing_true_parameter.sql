-- V12__fix_rls_missing_true_parameter.sql
-- Fixes an inconsistency in RLS policies across several tables.
-- V2 (tickets) and V11 (knowledge_articles) use current_setting('app.tenant_id', true)
-- which gracefully evaluates to NULL if the variable isn't set (fail-closed, 0 rows).
-- Other migrations (V3, V5, V6, V7, V9, V10) omitted the 'true' parameter.
-- Without 'true', querying these tables without a tenant context throws a
-- PostgreSQL exception ("unrecognized configuration parameter app.tenant_id").
-- This migration standardizes all RLS policies to use the 'true' parameter.

-- 1. users
DROP POLICY IF EXISTS users_tenant_isolation ON users;
CREATE POLICY users_tenant_isolation ON users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- 2. user_roles
DROP POLICY IF EXISTS user_roles_tenant_isolation ON user_roles;
CREATE POLICY user_roles_tenant_isolation ON user_roles
    USING (user_id IN (
        SELECT id FROM users
        WHERE tenant_id = current_setting('app.tenant_id', true)::uuid
    ));

-- 3. ticket_events
DROP POLICY IF EXISTS ticket_events_tenant_isolation ON ticket_events;
CREATE POLICY ticket_events_tenant_isolation ON ticket_events
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- 4. ticket_notes
DROP POLICY IF EXISTS ticket_notes_tenant_isolation ON ticket_notes;
CREATE POLICY ticket_notes_tenant_isolation ON ticket_notes
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- 5. notifications
DROP POLICY IF EXISTS notifications_tenant_isolation ON notifications;
CREATE POLICY notifications_tenant_isolation ON notifications
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- 6. response_templates
DROP POLICY IF EXISTS response_templates_tenant_isolation ON response_templates;
CREATE POLICY response_templates_tenant_isolation ON response_templates
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- 7. ticket_satisfaction
DROP POLICY IF EXISTS ticket_satisfaction_tenant_isolation ON ticket_satisfaction;
CREATE POLICY ticket_satisfaction_tenant_isolation ON ticket_satisfaction
    USING (ticket_id IN (
        SELECT id FROM tickets
        WHERE tenant_id = current_setting('app.tenant_id', true)::uuid
    ))
    WITH CHECK (ticket_id IN (
        SELECT id FROM tickets
        WHERE tenant_id = current_setting('app.tenant_id', true)::uuid
    ));

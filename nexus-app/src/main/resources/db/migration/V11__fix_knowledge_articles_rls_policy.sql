-- V11__fix_knowledge_articles_rls_policy.sql
-- Fixes a CRITICAL bug: the V4 RLS policy on knowledge_articles uses
-- current_setting('app.current_tenant_id') but the application
-- (TenantAwareDataSource) sets 'app.tenant_id' via SET LOCAL.
--
-- The mismatched variable name means the RLS policy NEVER matches,
-- causing knowledge_articles to return zero rows for all tenants
-- when accessed through the runtime role (nexus_app).
--
-- This migration drops the broken policy and creates a corrected one
-- using the same 'app.tenant_id' variable that tickets, ticket_events,
-- ticket_notes, notifications, response_templates, and
-- ticket_satisfaction all use.
--
-- NOTE: V4 is an already-applied migration and must NOT be edited.

-- Drop the broken policy (wrong variable name)
DROP POLICY IF EXISTS knowledge_articles_tenant_isolation ON knowledge_articles;

-- Create the corrected policy using the SAME session variable
-- that TenantAwareDataSource sets: app.tenant_id
--
-- FOR ALL = applies to SELECT, INSERT, UPDATE, DELETE
-- USING  = filters rows on SELECT, UPDATE, DELETE
-- WITH CHECK = validates rows on INSERT and UPDATE
-- TO nexus_app = only applies to the runtime role (superuser bypasses)
CREATE POLICY knowledge_articles_tenant_isolation
    ON knowledge_articles
    FOR ALL
    TO nexus_app
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::uuid);

COMMENT ON POLICY knowledge_articles_tenant_isolation ON knowledge_articles IS
    'Corrected tenant isolation policy. Uses app.tenant_id (matching TenantAwareDataSource SET LOCAL). Fail-closed: if unset, no rows visible. Fixed in V11 — V4 used wrong variable name.';

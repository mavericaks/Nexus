-- V2__rls_policies.sql
-- Phase 1: Row-Level Security + low-privilege runtime role.
--
-- After this migration:
--   nexus      = owner/migration role (Flyway uses this)
--   nexus_app  = runtime role (the Spring Boot app uses this)
--   tickets    = RLS-protected — queries only see the current tenant's rows

-- ─── 1. Create the low-privilege runtime role ───────────────────────
-- This role can read/write data but cannot create, alter, or drop tables.
-- The password here is for local dev only — production uses env variables.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'nexus_app') THEN
        CREATE ROLE nexus_app WITH LOGIN PASSWORD 'nexus_app_local';
    END IF;
END
$$;

-- ─── 2. Grant data-level permissions (CRUD, not DDL) ────────────────
GRANT CONNECT ON DATABASE nexus TO nexus_app;
GRANT USAGE ON SCHEMA public TO nexus_app;

-- CRUD on all existing tables (includes tenants, tickets, flyway_schema_history)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO nexus_app;

-- Also grant on any future tables Flyway creates
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO nexus_app;

-- If we ever use sequences, grant usage (we use UUIDs now, but future-proof)
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO nexus_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE ON SEQUENCES TO nexus_app;

-- ─── 3. Enable RLS on tenant-scoped tables ──────────────────────────
-- tickets is the primary tenant-scoped table.
-- tenants table doesn't have a tenant_id column (the id IS the tenant),
-- so it gets a different policy pattern if needed later.

ALTER TABLE tickets ENABLE ROW LEVEL SECURITY;

-- FORCE means RLS applies even to the table owner (nexus).
-- Without this, connecting as the owner silently bypasses all policies.
ALTER TABLE tickets FORCE ROW LEVEL SECURITY;

-- ─── 4. Create the tenant isolation policy ──────────────────────────
-- current_setting('app.tenant_id', true):
--   - Reads the transaction-local variable set by the app via SET LOCAL
--   - The 'true' parameter means return NULL if the variable isn't set
--     (instead of throwing an error)
--   - If NULL, the cast to UUID succeeds (NULL::uuid = NULL),
--     and tenant_id = NULL is always false in SQL — so zero rows
--     are returned. This is fail-closed: forget to set context = see nothing.

CREATE POLICY tenant_isolation ON tickets
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- USING:      filters rows on SELECT, UPDATE, DELETE
-- WITH CHECK: validates rows on INSERT, UPDATE (prevents inserting
--             a row for a different tenant)

COMMENT ON POLICY tenant_isolation ON tickets IS
    'Restricts all CRUD to rows matching the transaction-local app.tenant_id setting. Fail-closed: if unset, no rows visible.';

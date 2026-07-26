-- V3__users_and_roles.sql
-- Phase 3: Users + roles for JWT/RBAC authentication.
--
-- Design:
--   users table has a tenant_id FK (each user belongs to exactly one tenant).
--   user_roles is a join table (a user can have multiple roles within their tenant).
--   RLS on users table — same pattern as tickets: nexus_app only sees
--   users whose tenant_id matches the current session's app.tenant_id.

-- ─── Users ──────────────────────────────────────────────────────────

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Email must be globally unique (not just per-tenant) to avoid login ambiguity.
-- When a user logs in with "admin@acme.com", there must be exactly one match.
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- Tenant lookup index
CREATE INDEX idx_users_tenant_id ON users(tenant_id);

COMMENT ON TABLE users IS 'Application users. Each belongs to exactly one tenant. Password is BCrypt-hashed.';
COMMENT ON COLUMN users.email IS 'Globally unique. Used as the login identifier (JWT "sub" claim).';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash (cost 10). Never stored or logged in plaintext.';

-- ─── User Roles ─────────────────────────────────────────────────────
-- Separate table because a user can hold multiple roles (e.g., OWNER + ADMIN).
-- VARCHAR instead of enum to avoid ALTER TYPE migrations when adding roles.

CREATE TABLE user_roles (
    user_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

COMMENT ON TABLE user_roles IS 'RBAC roles: OWNER, ADMIN, AGENT. A user can hold multiple roles.';

-- ─── RLS on users ───────────────────────────────────────────────────
-- Same pattern as tickets: nexus_app only sees users in the current tenant.

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY users_tenant_isolation ON users
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Force RLS even for the table owner (safety net)
ALTER TABLE users FORCE ROW LEVEL SECURITY;

-- ─── RLS on user_roles ──────────────────────────────────────────────
-- user_roles doesn't have tenant_id directly — join through users.
-- This policy ensures nexus_app can only see roles for users in the current tenant.

ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_roles_tenant_isolation ON user_roles
    USING (user_id IN (
        SELECT id FROM users
        WHERE tenant_id = current_setting('app.tenant_id')::uuid
    ));

ALTER TABLE user_roles FORCE ROW LEVEL SECURITY;

-- ─── Seed demo users ────────────────────────────────────────────────
-- BCrypt hash for 'password123' (cost 10):
-- $2a$10$dXJ3SW6G7P50lGmMQoeGUORFrZCYgCPQKb6DQ.YeGAjqkc2gCmKHG

-- Acme Corp owner
INSERT INTO users (id, tenant_id, email, password_hash, name) VALUES
    ('cccc0000-0000-0000-0000-000000000001',
     'aaaa0000-0000-0000-0000-000000000001',
     'owner@acme.com',
     '$2a$10$dXJ3SW6G7P50lGmMQoeGUORFrZCYgCPQKb6DQ.YeGAjqkc2gCmKHG',
     'Alice Acme');

INSERT INTO user_roles (user_id, role) VALUES
    ('cccc0000-0000-0000-0000-000000000001', 'ROLE_OWNER');

-- Acme Corp agent
INSERT INTO users (id, tenant_id, email, password_hash, name) VALUES
    ('cccc0000-0000-0000-0000-000000000002',
     'aaaa0000-0000-0000-0000-000000000001',
     'agent@acme.com',
     '$2a$10$dXJ3SW6G7P50lGmMQoeGUORFrZCYgCPQKb6DQ.YeGAjqkc2gCmKHG',
     'Bob Agent');

INSERT INTO user_roles (user_id, role) VALUES
    ('cccc0000-0000-0000-0000-000000000002', 'ROLE_AGENT');

-- Beta Inc admin
INSERT INTO users (id, tenant_id, email, password_hash, name) VALUES
    ('cccc0000-0000-0000-0000-000000000003',
     'bbbb0000-0000-0000-0000-000000000002',
     'admin@beta.com',
     '$2a$10$dXJ3SW6G7P50lGmMQoeGUORFrZCYgCPQKb6DQ.YeGAjqkc2gCmKHG',
     'Charlie Beta');

INSERT INTO user_roles (user_id, role) VALUES
    ('cccc0000-0000-0000-0000-000000000003', 'ROLE_ADMIN');

-- V1__baseline_schema.sql
-- Phase 1: Create the core tenants and tickets tables.
-- This is the first migration — Flyway numbers them in order
-- and never runs the same one twice.

-- Enable pgvector extension for embedding storage (Phase 4).
-- Safe to call repeatedly — does nothing if already enabled.
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable uuid-ossp for uuid_generate_v4() if needed.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── Tenants ────────────────────────────────────────────────────────

CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    plan_tier   VARCHAR(50)  NOT NULL DEFAULT 'FREE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE tenants IS 'Each row is a company using Nexus. plan_tier drives Strategy pattern for rate limits and thresholds.';
COMMENT ON COLUMN tenants.slug IS 'URL-friendly unique identifier, e.g. "acme-corp". Used in API paths.';
COMMENT ON COLUMN tenants.plan_tier IS 'FREE, STARTER, PROFESSIONAL, ENTERPRISE — controls rate limits and confidence thresholds via Strategy pattern.';

-- ─── Tickets ────────────────────────────────────────────────────────

CREATE TABLE tickets (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subject          VARCHAR(500) NOT NULL,
    description      TEXT,
    status           VARCHAR(50)  NOT NULL DEFAULT 'NEW',
    priority         VARCHAR(50),
    category         VARCHAR(50),
    confidence_score DOUBLE PRECISION,
    ai_response      TEXT,
    assignee_id      UUID,
    version          INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index on tenant_id — every query in a multi-tenant system filters by tenant.
-- Without this, every tenant-scoped query does a full table scan.
CREATE INDEX idx_tickets_tenant_id ON tickets(tenant_id);

-- Composite index for the most common dashboard query:
-- "show me this tenant's tickets, filtered by status, newest first"
CREATE INDEX idx_tickets_tenant_status ON tickets(tenant_id, status);

COMMENT ON TABLE tickets IS 'Core entity — tenant-scoped support tickets. RLS policy (next migration) filters by tenant_id.';
COMMENT ON COLUMN tickets.status IS 'Matches TicketStatus enum: NEW, CLASSIFIED, AI_DRAFTED, AUTO_RESOLVED, ESCALATED, IN_PROGRESS, RESOLVED, CLOSED.';
COMMENT ON COLUMN tickets.priority IS 'Matches TicketPriority enum: LOW, MEDIUM, HIGH, CRITICAL. Nullable until AI or human assigns it.';
COMMENT ON COLUMN tickets.category IS 'Matches TicketCategory enum: BILLING, TECHNICAL, ACCOUNT, FEATURE_REQUEST, GENERAL. Nullable until AI classifies it.';
COMMENT ON COLUMN tickets.confidence_score IS 'AI triage confidence (0.0–1.0). Derived from retrieval similarity + structured output validation, NOT self-reported.';
COMMENT ON COLUMN tickets.version IS 'Optimistic locking — prevents silent overwrites when two agents edit the same ticket.';
COMMENT ON COLUMN tickets.assignee_id IS 'Human agent assigned after escalation. FK to users table added in Phase 3 (Security).';

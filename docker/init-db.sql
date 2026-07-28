-- Docker entrypoint init script.
-- Runs ONCE when the Postgres container is first created (fresh volume).
-- Creates the low-privilege nexus_app role so Spring Boot can connect
-- immediately, before Flyway has a chance to run.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'nexus_app') THEN
        CREATE ROLE nexus_app WITH LOGIN PASSWORD 'nexus_app_local';
    END IF;
END
$$;

-- Grant basic connect permissions (Flyway V2 grants table-level perms later)
GRANT CONNECT ON DATABASE nexus TO nexus_app;
GRANT USAGE ON SCHEMA public TO nexus_app;

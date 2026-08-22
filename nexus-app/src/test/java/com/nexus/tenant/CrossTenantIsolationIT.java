package com.nexus.tenant;

import org.junit.jupiter.api.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-tenant isolation integration tests using a real PostgreSQL
 * container with Flyway migrations applied.
 *
 * <p>These tests exercise the actual RLS policies at the database
 * level — not mocked Java conditionals. They use JDBC directly
 * against the {@code nexus_app} role (the runtime role) to prove
 * that tenant isolation is enforced by PostgreSQL itself.
 *
 * <p><b>What is tested:</b></p>
 * <ul>
 *   <li>Tenant A's tickets are invisible to Tenant B</li>
 *   <li>Tenant A's knowledge articles are invisible to Tenant B</li>
 *   <li>Tenant B cannot update Tenant A's tickets</li>
 *   <li>Tenant B cannot delete Tenant A's tickets</li>
 *   <li>Missing tenant context returns zero rows (fail-closed)</li>
 * </ul>
 *
 * <p><b>How:</b> The container runs Flyway migrations (V1–V11) via
 * the {@code nexus} superuser role. Tests then connect as {@code nexus_app}
 * and set {@code app.tenant_id} via {@code SET LOCAL} — the exact same
 * mechanism used by {@code TenantAwareDataSource} at runtime.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CrossTenantIsolationIT {

    // Deterministic UUIDs matching V1 seed data
    private static final String TENANT_A = "aaaa0000-0000-0000-0000-000000000001"; // Acme Corp
    private static final String TENANT_B = "bbbb0000-0000-0000-0000-000000000002"; // Beta Inc

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("nexus_test")
            .withUsername("nexus")      // superuser = Flyway migration role
            .withPassword("nexus");

    private static Connection superuserConn;
    private static String ticketA_Id;

    @BeforeAll
    static void runMigrations() throws Exception {
        superuserConn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        // Apply Flyway migrations using the superuser connection
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (superuserConn != null) superuserConn.close();
    }

    /**
     * Returns a connection as the nexus_app runtime role with the given
     * tenant context set — simulating what TenantAwareDataSource does.
     */
    private Connection appConnection(String tenantId) throws SQLException {
        Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), "nexus_app", "nexus_app_local");
        conn.setAutoCommit(false);
        if (tenantId != null) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");
            }
        }
        return conn;
    }

    // ──────────────────────────────────────────────────────────────────
    //  TEST A: Tenant A creates a ticket. Tenant B cannot retrieve it.
    // ──────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("TEST A: Tenant A creates ticket; Tenant B cannot retrieve it")
    void tenantA_ticket_invisible_to_tenantB() throws Exception {
        // Tenant A creates a ticket
        try (Connection connA = appConnection(TENANT_A)) {
            try (PreparedStatement ps = connA.prepareStatement(
                    "INSERT INTO tickets (tenant_id, subject, description, status) " +
                    "VALUES (?::uuid, 'Acme ticket', 'Detail', 'NEW') RETURNING id")) {
                ps.setString(1, TENANT_A);
                ResultSet rs = ps.executeQuery();
                assertThat(rs.next()).isTrue();
                ticketA_Id = rs.getString(1);
            }
            connA.commit();
        }

        // Tenant B tries to read it — should see 0 rows
        try (Connection connB = appConnection(TENANT_B)) {
            try (PreparedStatement ps = connB.prepareStatement(
                    "SELECT id FROM tickets WHERE id = ?::uuid")) {
                ps.setString(1, ticketA_Id);
                ResultSet rs = ps.executeQuery();
                assertThat(rs.next()).as("Tenant B must NOT see Tenant A's ticket").isFalse();
            }
            connB.rollback();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  TEST B: Tenant A's knowledge articles are invisible to Tenant B
    //          via vector search (simulated with a plain SELECT since
    //          embeddings may not be populated — the RLS policy is the
    //          same regardless of whether the query uses <=> or not).
    // ──────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("TEST B: Tenant A's knowledge articles invisible to Tenant B")
    void tenantA_knowledge_invisible_to_tenantB() throws Exception {
        // Tenant A's articles were seeded by V4 — verify Tenant A can see them
        try (Connection connA = appConnection(TENANT_A)) {
            try (Statement stmt = connA.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT count(*) FROM knowledge_articles");
                rs.next();
                int countA = rs.getInt(1);
                assertThat(countA).as("Tenant A must see their own KB articles").isGreaterThan(0);
            }
            connA.rollback();
        }

        // Tenant B must see zero
        try (Connection connB = appConnection(TENANT_B)) {
            try (Statement stmt = connB.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT count(*) FROM knowledge_articles");
                rs.next();
                int countB = rs.getInt(1);
                assertThat(countB).as("Tenant B must NOT see Tenant A's KB articles").isZero();
            }
            connB.rollback();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  TEST C: Tenant B cannot update Tenant A's ticket
    // ──────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("TEST C: Tenant B cannot update Tenant A's ticket")
    void tenantB_cannot_update_tenantA_ticket() throws Exception {
        try (Connection connB = appConnection(TENANT_B)) {
            try (PreparedStatement ps = connB.prepareStatement(
                    "UPDATE tickets SET subject = 'HACKED' WHERE id = ?::uuid")) {
                ps.setString(1, ticketA_Id);
                int rows = ps.executeUpdate();
                assertThat(rows).as("Tenant B update must affect 0 rows").isZero();
            }
            connB.rollback();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  TEST D: Tenant B cannot delete Tenant A's ticket
    // ──────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("TEST D: Tenant B cannot delete Tenant A's ticket")
    void tenantB_cannot_delete_tenantA_ticket() throws Exception {
        try (Connection connB = appConnection(TENANT_B)) {
            try (PreparedStatement ps = connB.prepareStatement(
                    "DELETE FROM tickets WHERE id = ?::uuid")) {
                ps.setString(1, ticketA_Id);
                int rows = ps.executeUpdate();
                assertThat(rows).as("Tenant B delete must affect 0 rows").isZero();
            }
            connB.rollback();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  TEST F: Missing tenant context returns zero rows (fail-closed)
    // ──────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("TEST F: No tenant context → zero rows (fail-closed)")
    void noTenantContext_seesNothing() throws Exception {
        // Connect as nexus_app WITHOUT setting app.tenant_id
        try (Connection conn = appConnection(null)) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT count(*) FROM tickets");
                rs.next();
                assertThat(rs.getInt(1)).as("No tenant context must return 0 tickets").isZero();
            }
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT count(*) FROM knowledge_articles");
                rs.next();
                assertThat(rs.getInt(1)).as("No tenant context must return 0 KB articles").isZero();
            }
            conn.rollback();
        }
    }
}

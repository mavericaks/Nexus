package com.nexus.ticket.infrastructure.persistence;

import com.nexus.ticket.domain.TicketCategory;
import com.nexus.ticket.domain.TicketPriority;
import com.nexus.ticket.domain.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable JPA Specifications for dynamic ticket filtering.
 *
 * <p>The ticket list endpoint supports optional filters: status, priority,
 * category. Instead of writing a separate query method for each combination
 * (8 combinations with 3 filters!), Specifications compose dynamically:
 *
 * <pre>
 * Specification&lt;TicketEntity&gt; spec = Specification.where(null);
 * if (status != null) spec = spec.and(TicketSpecifications.hasStatus(status));
 * if (priority != null) spec = spec.and(TicketSpecifications.hasPriority(priority));
 * // ... pass spec to repository.findAll(spec, pageable)
 * </pre>
 *
 * <p>Each method returns a {@code Specification} — a lambda that builds
 * a JPA {@code Predicate} (a WHERE clause fragment). Spring Data combines
 * them with AND at runtime.
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
        // Utility class — no instances
    }

    /** Filter tickets by status (e.g., only show NEW tickets). */
    public static Specification<TicketEntity> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Filter tickets by priority (e.g., only show CRITICAL tickets). */
    public static Specification<TicketEntity> hasPriority(TicketPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    /** Filter tickets by category (e.g., only show BILLING tickets). */
    public static Specification<TicketEntity> hasCategory(TicketCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }
}

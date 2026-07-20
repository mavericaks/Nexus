package com.nexus.ticket.application.dto;

import com.nexus.ticket.domain.TicketCategory;
import com.nexus.ticket.domain.TicketPriority;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;

/**
 * Maps between {@link TicketEntity} and DTOs.
 *
 * <p>Manual mapping (not MapStruct) — keeps it transparent,
 * debuggable, and avoids a code-generation dependency.
 * The mapper is a pure utility class with static methods.
 */
public final class TicketMapper {

    private TicketMapper() {
        // Utility class — no instances
    }

    /**
     * Converts a JPA entity to the outbound API response.
     * Enum values are converted to strings for JSON serialization.
     */
    public static TicketResponse toResponse(TicketEntity entity) {
        return new TicketResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getSubject(),
                entity.getDescription(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getPriority() != null ? entity.getPriority().name() : null,
                entity.getCategory() != null ? entity.getCategory().name() : null,
                entity.getConfidenceScore(),
                entity.getAiResponse(),
                entity.getAssigneeId(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Parses a category string into the domain enum.
     *
     * @return the parsed enum, or null if the input is null/blank
     * @throws IllegalArgumentException if the string doesn't match any enum value
     */
    public static TicketCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return TicketCategory.valueOf(category.toUpperCase());
    }

    /**
     * Parses a priority string into the domain enum.
     *
     * @return the parsed enum, or null if the input is null/blank
     * @throws IllegalArgumentException if the string doesn't match any enum value
     */
    public static TicketPriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        return TicketPriority.valueOf(priority.toUpperCase());
    }
}

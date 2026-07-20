package com.nexus.common.exception;

import java.time.OffsetDateTime;

/**
 * Standard error response shape for all API errors.
 *
 * <p>Every error the API returns — validation, not-found, illegal transition,
 * server error — uses this same JSON structure. Clients can parse errors
 * consistently without guessing the shape.
 *
 * <pre>
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Tenant not found: 550e8400-...",
 *   "timestamp": "2026-07-20T00:20:00+05:30"
 * }
 * </pre>
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        OffsetDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, OffsetDateTime.now());
    }
}

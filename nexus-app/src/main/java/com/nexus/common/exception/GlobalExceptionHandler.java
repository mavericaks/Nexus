package com.nexus.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler — maps every application exception to a
 * consistent {@link ErrorResponse} JSON shape with the correct HTTP status.
 *
 * <p>This centralizes error handling so controllers stay clean (no try/catch)
 * and clients always get the same error format regardless of what went wrong.
 *
 * <p>Handles:
 * <ul>
 *   <li>Bean Validation failures → 400 Bad Request</li>
 *   <li>Illegal enum values → 400 Bad Request</li>
 *   <li>Tenant not found → 404 Not Found</li>
 *   <li>Ticket not found → 404 Not Found</li>
 *   <li>Illegal state transition → 409 Conflict</li>
 *   <li>Optimistic lock conflict → 409 Conflict</li>
 *   <li>Everything else → 500 Internal Server Error</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation failures — triggered when {@code @Valid} on a controller
     * parameter finds constraint violations ({@code @NotBlank}, {@code @Size}, etc.).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.debug("Validation failed: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", message));
    }

    /**
     * Invalid enum values — e.g., category="INVALID" doesn't match any
     * {@code TicketCategory} value.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Invalid argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    /** Tenant ID in the URL doesn't match any tenant in the database. */
    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFound(TenantNotFoundException ex) {
        log.debug("Tenant not found: {}", ex.getTenantId());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    /**
     * Ticket not found — either it doesn't exist, or RLS hides it
     * because it belongs to a different tenant.
     */
    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFound(TicketNotFoundException ex) {
        log.debug("Ticket not found: {}", ex.getTicketId());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    /**
     * Illegal state transition — e.g., trying to move a CLOSED ticket
     * back to IN_PROGRESS.
     */
    @ExceptionHandler(IllegalTicketTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(IllegalTicketTransitionException ex) {
        log.debug("Illegal transition: {} → {}", ex.getFrom(), ex.getTo());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    /**
     * Optimistic locking conflict — two concurrent edits on the same ticket.
     * The second write gets this error; the client should re-read and retry.
     */
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict",
                        "This ticket was modified by another request. Please re-read and retry."));
    }

    /**
     * Catch-all for unexpected errors. Logs the full stack trace
     * but only returns a generic message to the client (don't leak internals).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "An unexpected error occurred. Please try again later."));
    }
}

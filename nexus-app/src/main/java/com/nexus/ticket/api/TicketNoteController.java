package com.nexus.ticket.api;

import com.nexus.ticket.application.TicketNoteService;
import com.nexus.ticket.application.dto.CreateNoteRequest;
import com.nexus.ticket.application.dto.NoteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for internal ticket notes (agent collaboration).
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets/{ticketId}/notes")
public class TicketNoteController {

    private final TicketNoteService ticketNoteService;

    public TicketNoteController(TicketNoteService ticketNoteService) {
        this.ticketNoteService = ticketNoteService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<NoteResponse> addNote(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID actorId = UUID.fromString(jwt.getClaimAsString("userId"));
        String actorName = jwt.getClaimAsString("sub");

        NoteResponse response = ticketNoteService.addNote(ticketId, request, actorId, actorName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<List<NoteResponse>> getNotes(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId) {

        List<NoteResponse> notes = ticketNoteService.getNotesForTicket(ticketId);
        return ResponseEntity.ok(notes);
    }
}

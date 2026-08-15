package com.nexus.ticket.application;

import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.ticket.application.dto.CreateNoteRequest;
import com.nexus.ticket.application.dto.NoteResponse;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;
import com.nexus.ticket.infrastructure.persistence.TicketNoteEntity;
import com.nexus.ticket.infrastructure.persistence.TicketNoteRepository;
import com.nexus.ticket.infrastructure.persistence.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for internal ticket notes (agent collaboration).
 */
@Service
@Transactional
public class TicketNoteService {

    private final TicketNoteRepository noteRepository;
    private final TicketRepository ticketRepository;

    public TicketNoteService(TicketNoteRepository noteRepository, TicketRepository ticketRepository) {
        this.noteRepository = noteRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Add an internal note to a ticket.
     */
    public NoteResponse addNote(UUID ticketId, CreateNoteRequest request,
                                UUID actorId, String actorName) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketNoteEntity note = new TicketNoteEntity(
                ticket, ticket.getTenant(), actorId, actorName, request.content());
        note = noteRepository.save(note);

        return toResponse(note);
    }

    /**
     * List all notes for a ticket, ordered chronologically.
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesForTicket(UUID ticketId) {
        return noteRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NoteResponse toResponse(TicketNoteEntity note) {
        return new NoteResponse(
                note.getId(),
                note.getTicketId(),
                note.getAuthorId(),
                note.getAuthorName(),
                note.getContent(),
                note.getCreatedAt());
    }
}

package com.nexus.ticket.api;

import com.nexus.ticket.application.ResponseTemplateService;
import com.nexus.ticket.application.dto.CreateTemplateRequest;
import com.nexus.ticket.application.dto.TemplateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for response template CRUD.
 * Only ADMIN and OWNER can create/update/delete. AGENT can read.
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/templates")
public class ResponseTemplateController {

    private final ResponseTemplateService templateService;

    public ResponseTemplateController(ResponseTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<TemplateResponse> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID createdBy = UUID.fromString(jwt.getClaimAsString("userId"));
        TemplateResponse response = templateService.create(tenantId, request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<List<TemplateResponse>> list(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String category) {

        List<TemplateResponse> templates = category != null
                ? templateService.listByCategory(category)
                : templateService.listAll();
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<TemplateResponse> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID templateId,
            @Valid @RequestBody CreateTemplateRequest request) {

        TemplateResponse response = templateService.update(templateId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tenantId,
            @PathVariable UUID templateId) {

        templateService.delete(templateId);
        return ResponseEntity.noContent().build();
    }
}

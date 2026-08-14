package com.nexus.ticket.application;

import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.ticket.application.dto.CreateTemplateRequest;
import com.nexus.ticket.application.dto.TemplateResponse;
import com.nexus.ticket.infrastructure.persistence.ResponseTemplateEntity;
import com.nexus.ticket.infrastructure.persistence.ResponseTemplateRepository;
import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import com.nexus.tenant.infrastructure.persistence.TenantRepository;
import com.nexus.common.exception.TenantNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for response template CRUD.
 *
 * <p>Templates are tenant-scoped — each tenant has their own library.
 * Only ADMIN and OWNER roles can create/update/delete templates.
 */
@Service
@Transactional
public class ResponseTemplateService {

    private final ResponseTemplateRepository templateRepository;
    private final TenantRepository tenantRepository;

    public ResponseTemplateService(ResponseTemplateRepository templateRepository,
                                    TenantRepository tenantRepository) {
        this.templateRepository = templateRepository;
        this.tenantRepository = tenantRepository;
    }

    public TemplateResponse create(UUID tenantId, CreateTemplateRequest request, UUID createdBy) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        ResponseTemplateEntity entity = new ResponseTemplateEntity(
                tenant, request.title(), request.content(), request.category(), createdBy);
        entity = templateRepository.save(entity);

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listAll() {
        return templateRepository.findAllByOrderByTitleAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listByCategory(String category) {
        return templateRepository.findByCategoryOrderByTitleAsc(category)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TemplateResponse update(UUID templateId, CreateTemplateRequest request) {
        ResponseTemplateEntity entity = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        entity.updateContent(request.title(), request.content(), request.category());
        entity = templateRepository.save(entity);

        return toResponse(entity);
    }

    public void delete(UUID templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        templateRepository.deleteById(templateId);
    }

    private TemplateResponse toResponse(ResponseTemplateEntity entity) {
        return new TemplateResponse(
                entity.getId(), entity.getTitle(), entity.getContent(),
                entity.getCategory(), entity.getCreatedBy(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

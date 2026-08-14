package com.nexus.ticket.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ResponseTemplateEntity}.
 * RLS handles tenant isolation.
 */
@Repository
public interface ResponseTemplateRepository extends JpaRepository<ResponseTemplateEntity, UUID> {

    List<ResponseTemplateEntity> findAllByOrderByTitleAsc();

    List<ResponseTemplateEntity> findByCategoryOrderByTitleAsc(String category);
}

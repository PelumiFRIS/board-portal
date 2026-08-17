package com.fris.boardportal.comment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtAsc(
            UUID organizationId, CommentEntityType entityType, UUID entityId);

    Optional<Comment> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

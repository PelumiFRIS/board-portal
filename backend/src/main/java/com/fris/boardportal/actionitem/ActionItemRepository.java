package com.fris.boardportal.actionitem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionItemRepository extends JpaRepository<ActionItem, UUID> {

    List<ActionItem> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<ActionItem> findByMeetingIdOrderByCreatedAtDesc(UUID meetingId);

    Optional<ActionItem> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

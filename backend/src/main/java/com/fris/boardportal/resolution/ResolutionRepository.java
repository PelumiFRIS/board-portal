package com.fris.boardportal.resolution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolutionRepository extends JpaRepository<Resolution, UUID> {

    List<Resolution> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Resolution> findByMeetingIdOrderByCreatedAtDesc(UUID meetingId);

    Optional<Resolution> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

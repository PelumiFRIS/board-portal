package com.fris.boardportal.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    List<Meeting> findByOrganizationIdOrderByScheduledStartDesc(UUID organizationId);

    Optional<Meeting> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

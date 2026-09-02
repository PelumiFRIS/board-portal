package com.fris.boardportal.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingTypeOptionRepository extends JpaRepository<MeetingTypeOption, UUID> {

    List<MeetingTypeOption> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<MeetingTypeOption> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}

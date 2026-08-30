package com.fris.boardportal.resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByOrganizationIdOrderByTitleAsc(UUID organizationId);

    Optional<Resource> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

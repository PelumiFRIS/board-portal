package com.fris.boardportal.compliance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceFilingRepository extends JpaRepository<ComplianceFiling, UUID> {

    List<ComplianceFiling> findByOrganizationIdOrderByDueDateAsc(UUID organizationId);

    Optional<ComplianceFiling> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ComplianceFiling> findByStatusAndDueDate(ComplianceFilingStatus status, LocalDate dueDate);
}

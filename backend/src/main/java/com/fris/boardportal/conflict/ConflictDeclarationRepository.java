package com.fris.boardportal.conflict;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConflictDeclarationRepository extends JpaRepository<ConflictDeclaration, UUID> {

    List<ConflictDeclaration> findByOrganizationIdOrderByDeclaredAtDesc(UUID organizationId);

    List<ConflictDeclaration> findByUserIdOrderByDeclaredAtDesc(UUID userId);
}

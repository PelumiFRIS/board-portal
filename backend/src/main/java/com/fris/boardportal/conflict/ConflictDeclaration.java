package com.fris.boardportal.conflict;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conflict_declarations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDeclaration {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "declared_by", nullable = false)
    private UUID declaredBy;

    @Column(name = "declared_by_name", nullable = false)
    private String declaredByName;

    @Column(name = "has_conflict", nullable = false)
    private boolean hasConflict;

    @Column
    private String details;

    @Column(name = "declared_at", nullable = false, updatable = false)
    private Instant declaredAt;

    public static ConflictDeclaration create(UUID organizationId, UUID userId, String userName, UUID declaredBy,
            String declaredByName, boolean hasConflict, String details) {
        ConflictDeclaration declaration = new ConflictDeclaration();
        declaration.setId(UUID.randomUUID());
        declaration.setOrganizationId(organizationId);
        declaration.setUserId(userId);
        declaration.setUserName(userName);
        declaration.setDeclaredBy(declaredBy);
        declaration.setDeclaredByName(declaredByName);
        declaration.setHasConflict(hasConflict);
        declaration.setDetails(details);
        declaration.setDeclaredAt(Instant.now());
        return declaration;
    }
}

package com.fris.boardportal.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compliance_filings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceFiling {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceFilingStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ComplianceFiling create(UUID organizationId, String title, String description, LocalDate dueDate,
            UUID createdBy) {
        ComplianceFiling filing = new ComplianceFiling();
        filing.setId(UUID.randomUUID());
        filing.setOrganizationId(organizationId);
        filing.setTitle(title);
        filing.setDescription(description);
        filing.setDueDate(dueDate);
        filing.setStatus(ComplianceFilingStatus.PENDING);
        filing.setCreatedBy(createdBy);
        Instant now = Instant.now();
        filing.setCreatedAt(now);
        filing.setUpdatedAt(now);
        return filing;
    }
}

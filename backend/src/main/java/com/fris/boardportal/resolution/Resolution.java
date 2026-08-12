package com.fris.boardportal.resolution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resolutions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resolution {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResolutionStatus status;

    @Enumerated(EnumType.STRING)
    @Column
    private ResolutionOutcome outcome;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public static Resolution create(UUID organizationId, UUID meetingId, String title, String description,
            UUID createdBy) {
        Resolution resolution = new Resolution();
        resolution.setId(UUID.randomUUID());
        resolution.setOrganizationId(organizationId);
        resolution.setMeetingId(meetingId);
        resolution.setTitle(title);
        resolution.setDescription(description);
        resolution.setStatus(ResolutionStatus.DRAFT);
        resolution.setCreatedBy(createdBy);
        resolution.setCreatedAt(Instant.now());
        return resolution;
    }
}

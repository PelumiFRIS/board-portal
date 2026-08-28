package com.fris.boardportal.committee;

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
@Table(name = "committees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Committee {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "parent_committee_id")
    private UUID parentCommitteeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Committee create(UUID organizationId, String name, String description, UUID parentCommitteeId) {
        Committee committee = new Committee();
        committee.setId(UUID.randomUUID());
        committee.setOrganizationId(organizationId);
        committee.setName(name);
        committee.setDescription(description);
        committee.setParentCommitteeId(parentCommitteeId);
        Instant now = Instant.now();
        committee.setCreatedAt(now);
        committee.setUpdatedAt(now);
        return committee;
    }
}

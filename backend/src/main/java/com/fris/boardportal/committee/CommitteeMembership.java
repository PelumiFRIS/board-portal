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
@Table(name = "committee_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeMembership {

    @Id
    private UUID id;

    @Column(name = "committee_id", nullable = false)
    private UUID committeeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "is_chair", nullable = false)
    private boolean chair;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static CommitteeMembership create(UUID committeeId, UUID userId) {
        CommitteeMembership membership = new CommitteeMembership();
        membership.setId(UUID.randomUUID());
        membership.setCommitteeId(committeeId);
        membership.setUserId(userId);
        membership.setChair(false);
        membership.setCreatedAt(Instant.now());
        return membership;
    }
}

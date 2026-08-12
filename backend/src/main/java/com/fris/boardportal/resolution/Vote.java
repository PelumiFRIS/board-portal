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
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vote {

    @Id
    private UUID id;

    @Column(name = "resolution_id", nullable = false)
    private UUID resolutionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteChoice choice;

    @Column(name = "cast_at", nullable = false)
    private Instant castAt;

    public static Vote create(UUID resolutionId, UUID userId, VoteChoice choice) {
        Vote vote = new Vote();
        vote.setId(UUID.randomUUID());
        vote.setResolutionId(resolutionId);
        vote.setUserId(userId);
        vote.setChoice(choice);
        vote.setCastAt(Instant.now());
        return vote;
    }
}

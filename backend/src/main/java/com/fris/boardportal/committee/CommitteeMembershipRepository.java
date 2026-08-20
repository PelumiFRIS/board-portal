package com.fris.boardportal.committee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitteeMembershipRepository extends JpaRepository<CommitteeMembership, UUID> {

    List<CommitteeMembership> findByCommitteeId(UUID committeeId);

    List<CommitteeMembership> findByUserId(UUID userId);

    Optional<CommitteeMembership> findByCommitteeIdAndUserId(UUID committeeId, UUID userId);

    void deleteByCommitteeIdAndUserId(UUID committeeId, UUID userId);

    void deleteByCommitteeId(UUID committeeId);
}

package com.fris.boardportal.resolution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    List<Vote> findByResolutionId(UUID resolutionId);

    Optional<Vote> findByResolutionIdAndUserId(UUID resolutionId, UUID userId);

    void deleteByResolutionId(UUID resolutionId);
}

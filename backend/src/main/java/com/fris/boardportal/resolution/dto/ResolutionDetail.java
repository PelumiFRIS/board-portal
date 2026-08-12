package com.fris.boardportal.resolution.dto;

import com.fris.boardportal.resolution.ResolutionOutcome;
import com.fris.boardportal.resolution.ResolutionStatus;
import com.fris.boardportal.resolution.VoteChoice;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResolutionDetail(
        UUID id,
        UUID meetingId,
        String title,
        String description,
        ResolutionStatus status,
        ResolutionOutcome outcome,
        long forCount,
        long againstCount,
        long abstainCount,
        VoteChoice myVote,
        Instant createdAt,
        Instant openedAt,
        Instant closedAt,
        List<VoteRecord> votes) {
}

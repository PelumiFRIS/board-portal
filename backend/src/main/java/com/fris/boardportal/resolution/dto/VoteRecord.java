package com.fris.boardportal.resolution.dto;

import com.fris.boardportal.resolution.VoteChoice;
import java.time.Instant;
import java.util.UUID;

public record VoteRecord(
        UUID voterId,
        String voterName,
        VoteChoice choice,
        Instant castAt) {
}

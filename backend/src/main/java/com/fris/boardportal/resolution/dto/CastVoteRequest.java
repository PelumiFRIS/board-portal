package com.fris.boardportal.resolution.dto;

import com.fris.boardportal.resolution.VoteChoice;
import jakarta.validation.constraints.NotNull;

public record CastVoteRequest(@NotNull VoteChoice choice) {
}

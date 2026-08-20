package com.fris.boardportal.committee.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddCommitteeMemberRequest(@NotNull UUID userId) {
}

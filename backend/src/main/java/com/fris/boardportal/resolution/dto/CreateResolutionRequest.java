package com.fris.boardportal.resolution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateResolutionRequest(
        @NotNull UUID meetingId,
        @NotBlank String title,
        String description) {
}

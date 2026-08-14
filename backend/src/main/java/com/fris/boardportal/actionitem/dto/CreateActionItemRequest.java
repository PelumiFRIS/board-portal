package com.fris.boardportal.actionitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateActionItemRequest(
        @NotNull UUID meetingId,
        @NotBlank String title,
        String description,
        @NotNull UUID assigneeId,
        LocalDate dueDate) {
}

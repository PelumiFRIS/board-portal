package com.fris.boardportal.actionitem.dto;

import com.fris.boardportal.actionitem.ActionItemStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActionItemSummary(
        UUID id,
        UUID meetingId,
        String title,
        String description,
        UUID assigneeId,
        String assigneeName,
        LocalDate dueDate,
        ActionItemStatus status,
        Instant createdAt) {
}

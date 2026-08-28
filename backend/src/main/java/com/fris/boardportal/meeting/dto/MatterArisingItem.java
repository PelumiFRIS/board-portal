package com.fris.boardportal.meeting.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MatterArisingItem(
        UUID id,
        String title,
        String description,
        String assigneeName,
        LocalDate dueDate,
        UUID sourceMeetingId,
        String sourceMeetingTitle,
        Instant sourceMeetingScheduledStart) {
}

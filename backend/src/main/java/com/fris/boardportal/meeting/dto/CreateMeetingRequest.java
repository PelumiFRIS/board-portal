package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.meeting.MeetingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateMeetingRequest(
        @NotBlank String title,
        String description,
        String location,
        @NotNull Instant scheduledStart,
        Instant scheduledEnd,
        UUID committeeId,
        MeetingType meetingType) {
}

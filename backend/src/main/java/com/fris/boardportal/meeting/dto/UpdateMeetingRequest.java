package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.meeting.MeetingStatus;
import java.time.Instant;
import java.util.UUID;

public record UpdateMeetingRequest(
        String title,
        String description,
        String location,
        Instant scheduledStart,
        Instant scheduledEnd,
        MeetingStatus status,
        String minutesContent,
        UUID committeeId,
        UUID meetingTypeId) {
}

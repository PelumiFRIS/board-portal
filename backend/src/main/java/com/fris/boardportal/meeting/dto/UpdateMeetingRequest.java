package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.meeting.MeetingStatus;
import java.time.Instant;

public record UpdateMeetingRequest(
        String title,
        String description,
        String location,
        Instant scheduledStart,
        Instant scheduledEnd,
        MeetingStatus status,
        String minutesContent) {
}

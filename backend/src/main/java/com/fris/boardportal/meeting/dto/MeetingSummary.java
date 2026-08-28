package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.meeting.MeetingStatus;
import java.time.Instant;
import java.util.UUID;

public record MeetingSummary(
        UUID id,
        String title,
        String location,
        Instant scheduledStart,
        Instant scheduledEnd,
        MeetingStatus status,
        UUID committeeId) {

    public static MeetingSummary from(Meeting meeting) {
        return new MeetingSummary(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getLocation(),
                meeting.getScheduledStart(),
                meeting.getScheduledEnd(),
                meeting.getStatus(),
                meeting.getCommitteeId());
    }
}

package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.meeting.MeetingStatus;
import com.fris.boardportal.meeting.MinutesStatus;
import java.time.Instant;
import java.util.UUID;

public record MeetingSummary(
        UUID id,
        String title,
        String location,
        Instant scheduledStart,
        Instant scheduledEnd,
        MeetingStatus status,
        MinutesStatus minutesStatus,
        UUID committeeId,
        UUID meetingTypeId,
        String meetingTypeName) {

    public static MeetingSummary from(Meeting meeting, String meetingTypeName) {
        return new MeetingSummary(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getLocation(),
                meeting.getScheduledStart(),
                meeting.getScheduledEnd(),
                meeting.getStatus(),
                meeting.getMinutesStatus(),
                meeting.getCommitteeId(),
                meeting.getMeetingTypeId(),
                meetingTypeName);
    }
}

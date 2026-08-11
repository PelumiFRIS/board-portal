package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.meeting.MeetingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeetingDetail(
        UUID id,
        String title,
        String description,
        String location,
        Instant scheduledStart,
        Instant scheduledEnd,
        MeetingStatus status,
        String minutesContent,
        List<AgendaItemDto> agendaItems,
        List<DocumentSummary> documents) {

    public static MeetingDetail from(Meeting meeting, List<AgendaItemDto> agendaItems, List<DocumentSummary> documents) {
        return new MeetingDetail(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getLocation(),
                meeting.getScheduledStart(),
                meeting.getScheduledEnd(),
                meeting.getStatus(),
                meeting.getMinutesContent(),
                agendaItems,
                documents);
    }
}

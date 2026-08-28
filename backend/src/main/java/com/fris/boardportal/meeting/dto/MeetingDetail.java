package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.meeting.MeetingStatus;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
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
        UUID committeeId,
        List<AgendaItemDto> agendaItems,
        List<DocumentSummary> documents,
        List<ResolutionSummary> resolutions,
        List<ActionItemSummary> actionItems) {

    public static MeetingDetail from(Meeting meeting, List<AgendaItemDto> agendaItems, List<DocumentSummary> documents,
            List<ResolutionSummary> resolutions, List<ActionItemSummary> actionItems) {
        return new MeetingDetail(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getLocation(),
                meeting.getScheduledStart(),
                meeting.getScheduledEnd(),
                meeting.getStatus(),
                meeting.getMinutesContent(),
                meeting.getCommitteeId(),
                agendaItems,
                documents,
                resolutions,
                actionItems);
    }
}

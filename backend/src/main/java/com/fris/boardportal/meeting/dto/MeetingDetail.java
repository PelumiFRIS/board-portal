package com.fris.boardportal.meeting.dto;

import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.meeting.MeetingStatus;
import com.fris.boardportal.meeting.MinutesStatus;
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
        MinutesStatus minutesStatus,
        UUID committeeId,
        UUID meetingTypeId,
        String meetingTypeName,
        List<AgendaItemDto> agendaItems,
        List<DocumentSummary> documents,
        List<ResolutionSummary> resolutions,
        List<ActionItemSummary> actionItems) {

    /**
     * @param minutesVisible whether the requester may see minutesContent — false for anyone but the
     *                       Company Secretary while minutes are still DRAFT, per FRIS's governance model.
     */
    public static MeetingDetail from(Meeting meeting, String meetingTypeName, List<AgendaItemDto> agendaItems,
            List<DocumentSummary> documents, List<ResolutionSummary> resolutions, List<ActionItemSummary> actionItems,
            boolean minutesVisible) {
        return new MeetingDetail(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getLocation(),
                meeting.getScheduledStart(),
                meeting.getScheduledEnd(),
                meeting.getStatus(),
                minutesVisible ? meeting.getMinutesContent() : null,
                meeting.getMinutesStatus(),
                meeting.getCommitteeId(),
                meeting.getMeetingTypeId(),
                meetingTypeName,
                agendaItems,
                documents,
                resolutions,
                actionItems);
    }
}

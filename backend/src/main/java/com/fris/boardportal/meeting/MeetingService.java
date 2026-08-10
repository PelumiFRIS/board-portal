package com.fris.boardportal.meeting;

import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.meeting.dto.AgendaItemDto;
import com.fris.boardportal.meeting.dto.CreateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingDetail;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.meeting.dto.UpdateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.UpdateMeetingRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final AgendaItemRepository agendaItemRepository;

    public MeetingService(MeetingRepository meetingRepository, AgendaItemRepository agendaItemRepository) {
        this.meetingRepository = meetingRepository;
        this.agendaItemRepository = agendaItemRepository;
    }

    public List<MeetingSummary> listForOrganization(AppUserPrincipal principal) {
        return meetingRepository.findByOrganizationIdOrderByScheduledStartDesc(principal.getOrganizationId()).stream()
                .map(MeetingSummary::from)
                .toList();
    }

    public MeetingDetail getDetail(AppUserPrincipal principal, UUID meetingId) {
        Meeting meeting = findMeetingInOrg(principal, meetingId);
        return toDetail(meeting);
    }

    @Transactional
    public MeetingSummary create(AppUserPrincipal admin, CreateMeetingRequest request) {
        Meeting meeting = Meeting.create(
                admin.getOrganizationId(),
                admin.getUserId(),
                request.title(),
                request.description(),
                request.location(),
                request.scheduledStart(),
                request.scheduledEnd());
        meetingRepository.save(meeting);
        return MeetingSummary.from(meeting);
    }

    @Transactional
    public MeetingDetail update(AppUserPrincipal admin, UUID meetingId, UpdateMeetingRequest request) {
        Meeting meeting = findMeetingInOrg(admin, meetingId);

        if (request.title() != null) {
            meeting.setTitle(request.title());
        }
        if (request.description() != null) {
            meeting.setDescription(request.description());
        }
        if (request.location() != null) {
            meeting.setLocation(request.location());
        }
        if (request.scheduledStart() != null) {
            meeting.setScheduledStart(request.scheduledStart());
        }
        if (request.scheduledEnd() != null) {
            meeting.setScheduledEnd(request.scheduledEnd());
        }
        if (request.status() != null) {
            meeting.setStatus(request.status());
        }
        if (request.minutesContent() != null) {
            meeting.setMinutesContent(request.minutesContent());
        }
        meeting.setUpdatedAt(Instant.now());
        meetingRepository.save(meeting);
        return toDetail(meeting);
    }

    @Transactional
    public AgendaItemDto addAgendaItem(AppUserPrincipal admin, UUID meetingId, CreateAgendaItemRequest request) {
        Meeting meeting = findMeetingInOrg(admin, meetingId);
        int position = request.position() != null
                ? request.position()
                : agendaItemRepository.countByMeetingId(meeting.getId());
        AgendaItem item = AgendaItem.create(meeting.getId(), position, request.title(), request.description());
        agendaItemRepository.save(item);
        return AgendaItemDto.from(item);
    }

    @Transactional
    public AgendaItemDto updateAgendaItem(AppUserPrincipal admin, UUID meetingId, UUID itemId,
            UpdateAgendaItemRequest request) {
        Meeting meeting = findMeetingInOrg(admin, meetingId);
        AgendaItem item = agendaItemRepository.findByIdAndMeetingId(itemId, meeting.getId())
                .orElseThrow(() -> ApiException.notFound("Agenda item not found"));

        if (request.title() != null) {
            item.setTitle(request.title());
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }
        if (request.position() != null) {
            item.setPosition(request.position());
        }
        item.setUpdatedAt(Instant.now());
        agendaItemRepository.save(item);
        return AgendaItemDto.from(item);
    }

    @Transactional
    public void deleteAgendaItem(AppUserPrincipal admin, UUID meetingId, UUID itemId) {
        Meeting meeting = findMeetingInOrg(admin, meetingId);
        AgendaItem item = agendaItemRepository.findByIdAndMeetingId(itemId, meeting.getId())
                .orElseThrow(() -> ApiException.notFound("Agenda item not found"));
        agendaItemRepository.delete(item);
    }

    private Meeting findMeetingInOrg(AppUserPrincipal principal, UUID meetingId) {
        return meetingRepository.findByIdAndOrganizationId(meetingId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Meeting not found"));
    }

    private MeetingDetail toDetail(Meeting meeting) {
        List<AgendaItemDto> agendaItems = agendaItemRepository.findByMeetingIdOrderByPositionAsc(meeting.getId())
                .stream()
                .map(AgendaItemDto::from)
                .toList();
        return MeetingDetail.from(meeting, agendaItems);
    }
}

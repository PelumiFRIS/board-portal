package com.fris.boardportal.meeting;

import com.fris.boardportal.actionitem.ActionItemService;
import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.document.DocumentRepository;
import com.fris.boardportal.meeting.dto.AgendaItemDto;
import com.fris.boardportal.meeting.dto.CreateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingDetail;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.meeting.dto.UpdateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.UpdateMeetingRequest;
import com.fris.boardportal.notification.EmailNotificationService;
import com.fris.boardportal.resolution.ResolutionService;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import com.fris.boardportal.user.UserStatus;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final AgendaItemRepository agendaItemRepository;
    private final DocumentRepository documentRepository;
    private final ResolutionService resolutionService;
    private final ActionItemService actionItemService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public MeetingService(MeetingRepository meetingRepository, AgendaItemRepository agendaItemRepository,
            DocumentRepository documentRepository, ResolutionService resolutionService,
            ActionItemService actionItemService, AuditLogService auditLogService, UserRepository userRepository,
            EmailNotificationService emailNotificationService) {
        this.meetingRepository = meetingRepository;
        this.agendaItemRepository = agendaItemRepository;
        this.documentRepository = documentRepository;
        this.resolutionService = resolutionService;
        this.actionItemService = actionItemService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    public List<MeetingSummary> listForOrganization(AppUserPrincipal principal) {
        return meetingRepository.findByOrganizationIdOrderByScheduledStartDesc(principal.getOrganizationId()).stream()
                .map(MeetingSummary::from)
                .toList();
    }

    public MeetingDetail getDetail(AppUserPrincipal principal, UUID meetingId) {
        Meeting meeting = findMeetingInOrg(principal, meetingId);
        return toDetail(meeting, principal);
    }

    public byte[] generateIcs(AppUserPrincipal principal, UUID meetingId) {
        Meeting meeting = findMeetingInOrg(principal, meetingId);

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//FirstRegistrars Board Portal//EN\r\n");
        ics.append(buildEventBlock(meeting));
        ics.append("END:VCALENDAR\r\n");

        return ics.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateFeed(String token) {
        User user = userRepository.findByCalendarToken(token)
                .orElseThrow(() -> ApiException.notFound("Calendar feed not found"));
        List<Meeting> meetings = meetingRepository.findByOrganizationIdOrderByScheduledStartDesc(
                user.getOrganizationId());

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//FirstRegistrars Board Portal//EN\r\n");
        for (Meeting meeting : meetings) {
            ics.append(buildEventBlock(meeting));
        }
        ics.append("END:VCALENDAR\r\n");

        return ics.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String buildEventBlock(Meeting meeting) {
        DateTimeFormatter icsFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
        Instant end = meeting.getScheduledEnd() != null
                ? meeting.getScheduledEnd()
                : meeting.getScheduledStart().plus(1, ChronoUnit.HOURS);

        StringBuilder event = new StringBuilder();
        event.append("BEGIN:VEVENT\r\n");
        event.append("UID:").append(meeting.getId()).append("@board-portal\r\n");
        event.append("DTSTAMP:").append(icsFormat.format(Instant.now())).append("\r\n");
        event.append("DTSTART:").append(icsFormat.format(meeting.getScheduledStart())).append("\r\n");
        event.append("DTEND:").append(icsFormat.format(end)).append("\r\n");
        event.append("SUMMARY:").append(icsEscape(meeting.getTitle())).append("\r\n");
        if (meeting.getLocation() != null) {
            event.append("LOCATION:").append(icsEscape(meeting.getLocation())).append("\r\n");
        }
        if (meeting.getDescription() != null) {
            event.append("DESCRIPTION:").append(icsEscape(meeting.getDescription())).append("\r\n");
        }
        event.append("END:VEVENT\r\n");
        return event.toString();
    }

    private String icsEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
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

        auditLogService.record(admin, AuditAction.MEETING_CREATED, AuditEntityType.MEETING, meeting.getId(),
                "Scheduled meeting \"" + meeting.getTitle() + "\"");

        List<User> activeMembers = userRepository.findByOrganizationId(admin.getOrganizationId()).stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .toList();
        emailNotificationService.notifyMeetingScheduled(meeting, activeMembers);

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
        String summary = request.status() != null
                ? "Marked meeting \"" + meeting.getTitle() + "\" as " + request.status()
                : "Updated meeting \"" + meeting.getTitle() + "\"";
        if (request.status() != null) {
            meeting.setStatus(request.status());
        }
        if (request.minutesContent() != null) {
            meeting.setMinutesContent(request.minutesContent());
        }
        meeting.setUpdatedAt(Instant.now());
        meetingRepository.save(meeting);

        auditLogService.record(admin, AuditAction.MEETING_UPDATED, AuditEntityType.MEETING, meeting.getId(), summary);

        return toDetail(meeting, admin);
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

    private MeetingDetail toDetail(Meeting meeting, AppUserPrincipal principal) {
        List<AgendaItemDto> agendaItems = agendaItemRepository.findByMeetingIdOrderByPositionAsc(meeting.getId())
                .stream()
                .map(AgendaItemDto::from)
                .toList();
        var documents = documentRepository.findSummariesByMeetingId(meeting.getId());
        var resolutions = resolutionService.listForMeeting(principal, meeting.getId());
        var actionItems = actionItemService.listForMeeting(principal, meeting.getId());
        return MeetingDetail.from(meeting, agendaItems, documents, resolutions, actionItems);
    }
}

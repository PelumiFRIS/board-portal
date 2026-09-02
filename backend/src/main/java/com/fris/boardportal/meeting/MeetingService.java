package com.fris.boardportal.meeting;

import com.fris.boardportal.actionitem.ActionItemService;
import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.committee.Committee;
import com.fris.boardportal.committee.CommitteeMembership;
import com.fris.boardportal.committee.CommitteeMembershipRepository;
import com.fris.boardportal.committee.CommitteeRepository;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.document.DocumentRepository;
import com.fris.boardportal.meeting.dto.AgendaItemDto;
import com.fris.boardportal.meeting.dto.CreateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MatterArisingItem;
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
import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.meeting.dto.AgendaItemDto;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingService {

    private static final DateTimeFormatter RECORD_WHEN_FORMAT = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneOffset.UTC);

    private final MeetingRepository meetingRepository;
    private final AgendaItemRepository agendaItemRepository;
    private final DocumentRepository documentRepository;
    private final ResolutionService resolutionService;
    private final ActionItemService actionItemService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private final CommitteeRepository committeeRepository;
    private final CommitteeMembershipRepository committeeMembershipRepository;
    private final MeetingTypeOptionRepository meetingTypeOptionRepository;

    public MeetingService(MeetingRepository meetingRepository, AgendaItemRepository agendaItemRepository,
            DocumentRepository documentRepository, ResolutionService resolutionService,
            ActionItemService actionItemService, AuditLogService auditLogService, UserRepository userRepository,
            EmailNotificationService emailNotificationService, CommitteeRepository committeeRepository,
            CommitteeMembershipRepository committeeMembershipRepository,
            MeetingTypeOptionRepository meetingTypeOptionRepository) {
        this.meetingRepository = meetingRepository;
        this.agendaItemRepository = agendaItemRepository;
        this.documentRepository = documentRepository;
        this.resolutionService = resolutionService;
        this.actionItemService = actionItemService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
        this.committeeRepository = committeeRepository;
        this.committeeMembershipRepository = committeeMembershipRepository;
        this.meetingTypeOptionRepository = meetingTypeOptionRepository;
    }

    public List<MeetingSummary> listForOrganization(AppUserPrincipal principal, UUID committeeId) {
        Map<UUID, String> typeNamesById = meetingTypeNamesById(principal.getOrganizationId());
        return meetingRepository.findByOrganizationIdOrderByScheduledStartDesc(principal.getOrganizationId()).stream()
                .filter(m -> committeeId == null || committeeId.equals(m.getCommitteeId()))
                .map(m -> MeetingSummary.from(m, typeNamesById.get(m.getMeetingTypeId())))
                .toList();
    }

    private Map<UUID, String> meetingTypeNamesById(UUID organizationId) {
        return meetingTypeOptionRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .collect(Collectors.toMap(MeetingTypeOption::getId, MeetingTypeOption::getName));
    }

    private String findMeetingTypeName(AppUserPrincipal principal, UUID meetingTypeId) {
        return meetingTypeOptionRepository.findByIdAndOrganizationId(meetingTypeId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Meeting type not found"))
                .getName();
    }

    public MeetingDetail getDetail(AppUserPrincipal principal, UUID meetingId) {
        Meeting meeting = findMeetingInOrg(principal, meetingId);
        return toDetail(meeting, principal);
    }

    public List<MatterArisingItem> getMattersArising(AppUserPrincipal principal, UUID meetingId) {
        Meeting meeting = findMeetingInOrg(principal, meetingId);
        Map<UUID, Meeting> meetingsById = meetingRepository
                .findByOrganizationIdOrderByScheduledStartDesc(principal.getOrganizationId()).stream()
                .collect(Collectors.toMap(Meeting::getId, m -> m));

        return actionItemService.listOpenExcludingMeeting(principal, meetingId).stream()
                .filter(item -> {
                    Meeting source = meetingsById.get(item.meetingId());
                    return source != null && Objects.equals(source.getCommitteeId(), meeting.getCommitteeId());
                })
                .sorted(Comparator.comparing(ActionItemSummary::dueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> {
                    Meeting source = meetingsById.get(item.meetingId());
                    return new MatterArisingItem(item.id(), item.title(), item.description(), item.assigneeName(),
                            item.dueDate(), source.getId(), source.getTitle(), source.getScheduledStart());
                })
                .toList();
    }

    public byte[] exportRecordHtml(AppUserPrincipal principal, UUID meetingId) {
        MeetingDetail detail = getDetail(principal, meetingId);

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">");
        html.append("<title>").append(htmlEscape(detail.title())).append("</title>");
        html.append("<style>");
        html.append("body{font-family:Georgia,'Times New Roman',serif;max-width:800px;margin:2rem auto;padding:0 1rem;color:#1a1a1a;}");
        html.append("h1{font-size:1.5rem;margin-bottom:0.25rem;}");
        html.append("h2{font-size:1.1rem;margin-top:2rem;border-bottom:1px solid #ccc;padding-bottom:0.25rem;}");
        html.append(".meta{color:#555;margin-bottom:1.5rem;}");
        html.append("table{width:100%;border-collapse:collapse;margin-top:0.5rem;}");
        html.append("th,td{text-align:left;padding:0.4rem 0.6rem;border-bottom:1px solid #ddd;font-size:0.95rem;}");
        html.append("th{color:#555;font-weight:600;}");
        html.append(".minutes{white-space:pre-wrap;line-height:1.5;}");
        html.append("@media print{body{margin:0;}}");
        html.append("</style></head><body>");

        html.append("<h1>").append(htmlEscape(detail.title())).append("</h1>");
        html.append("<p class=\"meta\">");
        html.append(RECORD_WHEN_FORMAT.format(detail.scheduledStart())).append(" UTC");
        if (detail.location() != null) {
            html.append(" &middot; ").append(htmlEscape(detail.location()));
        }
        html.append(" &middot; ").append(htmlEscape(detail.status().toString()));
        html.append("</p>");

        if (!detail.agendaItems().isEmpty()) {
            html.append("<h2>Agenda</h2><ol>");
            for (AgendaItemDto item : detail.agendaItems()) {
                html.append("<li>").append(htmlEscape(item.title()));
                if (item.description() != null) {
                    html.append(" — ").append(htmlEscape(item.description()));
                }
                html.append("</li>");
            }
            html.append("</ol>");
        }

        html.append("<h2>Minutes</h2>");
        html.append("<div class=\"minutes\">")
                .append(detail.minutesContent() != null ? htmlEscape(detail.minutesContent()) : "No minutes recorded.")
                .append("</div>");

        if (!detail.resolutions().isEmpty()) {
            html.append("<h2>Resolutions</h2><table><thead><tr>")
                    .append("<th>Title</th><th>Status</th><th>Outcome</th><th>For</th><th>Against</th><th>Abstain</th>")
                    .append("</tr></thead><tbody>");
            for (ResolutionSummary r : detail.resolutions()) {
                html.append("<tr>")
                        .append("<td>").append(htmlEscape(r.title())).append("</td>")
                        .append("<td>").append(htmlEscape(r.status().toString())).append("</td>")
                        .append("<td>").append(r.outcome() != null ? htmlEscape(r.outcome().toString()) : "—").append("</td>")
                        .append("<td>").append(r.forCount()).append("</td>")
                        .append("<td>").append(r.againstCount()).append("</td>")
                        .append("<td>").append(r.abstainCount()).append("</td>")
                        .append("</tr>");
            }
            html.append("</tbody></table>");
        }

        if (!detail.actionItems().isEmpty()) {
            html.append("<h2>Action items</h2><table><thead><tr>")
                    .append("<th>Title</th><th>Assignee</th><th>Due date</th><th>Status</th>")
                    .append("</tr></thead><tbody>");
            for (ActionItemSummary item : detail.actionItems()) {
                html.append("<tr>")
                        .append("<td>").append(htmlEscape(item.title())).append("</td>")
                        .append("<td>").append(htmlEscape(item.assigneeName())).append("</td>")
                        .append("<td>").append(item.dueDate() != null ? item.dueDate().toString() : "—").append("</td>")
                        .append("<td>").append(htmlEscape(item.status().toString())).append("</td>")
                        .append("</tr>");
            }
            html.append("</tbody></table>");
        }

        html.append("</body></html>");
        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String htmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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
        Committee committee = request.committeeId() != null
                ? findCommitteeInOrg(admin, request.committeeId())
                : null;
        String meetingTypeName = findMeetingTypeName(admin, request.meetingTypeId());

        Meeting meeting = Meeting.create(
                admin.getOrganizationId(),
                admin.getUserId(),
                request.title(),
                request.description(),
                request.location(),
                request.scheduledStart(),
                request.scheduledEnd(),
                request.committeeId(),
                request.meetingTypeId());
        meetingRepository.save(meeting);

        String summary = committee != null
                ? "Scheduled meeting \"" + meeting.getTitle() + "\" for committee \"" + committee.getName() + "\""
                : "Scheduled meeting \"" + meeting.getTitle() + "\"";
        summary = summary + " (" + meetingTypeName + ")";
        auditLogService.record(admin, AuditAction.MEETING_CREATED, AuditEntityType.MEETING, meeting.getId(), summary);

        emailNotificationService.notifyMeetingScheduled(meeting, resolveRecipients(admin, meeting));

        return MeetingSummary.from(meeting, meetingTypeName);
    }

    private List<User> resolveRecipients(AppUserPrincipal admin, Meeting meeting) {
        if (meeting.getCommitteeId() != null) {
            List<UUID> memberIds = committeeMembershipRepository.findByCommitteeId(meeting.getCommitteeId()).stream()
                    .map(CommitteeMembership::getUserId)
                    .toList();
            return userRepository.findAllById(memberIds).stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .toList();
        }
        return userRepository.findByOrganizationId(admin.getOrganizationId()).stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .toList();
    }

    private Committee findCommitteeInOrg(AppUserPrincipal principal, UUID committeeId) {
        return committeeRepository.findByIdAndOrganizationId(committeeId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Committee not found"));
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
        if (request.committeeId() != null) {
            findCommitteeInOrg(admin, request.committeeId());
            meeting.setCommitteeId(request.committeeId());
        }
        if (request.meetingTypeId() != null) {
            findMeetingTypeName(admin, request.meetingTypeId());
            meeting.setMeetingTypeId(request.meetingTypeId());
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
        String meetingTypeName = findMeetingTypeName(principal, meeting.getMeetingTypeId());
        return MeetingDetail.from(meeting, meetingTypeName, agendaItems, documents, resolutions, actionItems);
    }
}

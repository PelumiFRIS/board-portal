package com.fris.boardportal.actionitem;

import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.actionitem.dto.CreateActionItemRequest;
import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.common.CsvSupport;
import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.meeting.MeetingRepository;
import com.fris.boardportal.notification.EmailNotificationService;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionItemService {

    private final ActionItemRepository actionItemRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final EmailNotificationService emailNotificationService;

    public ActionItemService(ActionItemRepository actionItemRepository, MeetingRepository meetingRepository,
            UserRepository userRepository, AuditLogService auditLogService,
            EmailNotificationService emailNotificationService) {
        this.actionItemRepository = actionItemRepository;
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.emailNotificationService = emailNotificationService;
    }

    public List<ActionItemSummary> listForOrganization(AppUserPrincipal principal, UUID meetingId) {
        List<ActionItem> items = actionItemRepository.findByOrganizationIdOrderByCreatedAtDesc(
                principal.getOrganizationId());
        Map<UUID, User> usersById = usersById(principal.getOrganizationId());
        return items.stream()
                .filter(i -> meetingId == null || meetingId.equals(i.getMeetingId()))
                .map(i -> toSummary(i, usersById))
                .toList();
    }

    public List<ActionItemSummary> listForMeeting(AppUserPrincipal principal, UUID meetingId) {
        Map<UUID, User> usersById = usersById(principal.getOrganizationId());
        return actionItemRepository.findByMeetingIdOrderByCreatedAtDesc(meetingId).stream()
                .map(i -> toSummary(i, usersById))
                .toList();
    }

    public List<ActionItemSummary> listOpenExcludingMeeting(AppUserPrincipal principal, UUID excludeMeetingId) {
        Map<UUID, User> usersById = usersById(principal.getOrganizationId());
        return actionItemRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.getOrganizationId()).stream()
                .filter(i -> i.getStatus() == ActionItemStatus.OPEN)
                .filter(i -> !i.getMeetingId().equals(excludeMeetingId))
                .map(i -> toSummary(i, usersById))
                .toList();
    }

    public byte[] exportCsv(AppUserPrincipal admin) {
        Map<UUID, String> meetingTitlesById = meetingRepository
                .findByOrganizationIdOrderByScheduledStartDesc(admin.getOrganizationId()).stream()
                .collect(Collectors.toMap(Meeting::getId, Meeting::getTitle));

        StringBuilder csv = new StringBuilder();
        csv.append("Meeting,Title,Assignee,Due Date,Status\n");
        for (ActionItemSummary item : listForOrganization(admin, null)) {
            csv.append(CsvSupport.escapeField(meetingTitlesById.getOrDefault(item.meetingId(), "Unknown"))).append(',')
                    .append(CsvSupport.escapeField(item.title())).append(',')
                    .append(CsvSupport.escapeField(item.assigneeName())).append(',')
                    .append(CsvSupport.escapeField(item.dueDate() != null ? item.dueDate().toString() : "")).append(',')
                    .append(CsvSupport.escapeField(item.status().toString())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public ActionItemSummary create(AppUserPrincipal admin, CreateActionItemRequest request) {
        Meeting meeting = meetingRepository.findByIdAndOrganizationId(request.meetingId(), admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Meeting not found"));
        User assignee = userRepository.findByIdAndOrganizationId(request.assigneeId(), admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Assignee not found"));

        ActionItem item = ActionItem.create(admin.getOrganizationId(), request.meetingId(), request.title(),
                request.description(), request.assigneeId(), request.dueDate(), admin.getUserId());
        actionItemRepository.save(item);

        auditLogService.record(admin, AuditAction.ACTION_ITEM_CREATED, AuditEntityType.ACTION_ITEM, item.getId(),
                "Assigned \"" + item.getTitle() + "\" to " + assignee.getFirstName() + " " + assignee.getLastName());

        emailNotificationService.notifyActionItemAssigned(item, assignee, meeting.getTitle());

        return toSummary(item, assignee);
    }

    @Transactional
    public ActionItemSummary updateStatus(AppUserPrincipal principal, UUID id, ActionItemStatus status) {
        ActionItem item = findInOrg(principal, id);
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        boolean isAssignee = item.getAssigneeId().equals(principal.getUserId());
        if (!isAdmin && !isAssignee) {
            throw ApiException.forbidden("Only the assignee or an admin can update this action item");
        }

        item.setStatus(status);
        item.setUpdatedAt(Instant.now());
        actionItemRepository.save(item);

        Map<UUID, User> usersById = usersById(principal.getOrganizationId());
        auditLogService.record(principal, AuditAction.ACTION_ITEM_STATUS_CHANGED, AuditEntityType.ACTION_ITEM,
                item.getId(), "Marked \"" + item.getTitle() + "\" as " + status);

        return toSummary(item, usersById);
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID id) {
        ActionItem item = findInOrg(admin, id);
        actionItemRepository.delete(item);

        auditLogService.record(admin, AuditAction.ACTION_ITEM_DELETED, AuditEntityType.ACTION_ITEM, item.getId(),
                "Deleted action item \"" + item.getTitle() + "\"");
    }

    private ActionItem findInOrg(AppUserPrincipal principal, UUID id) {
        return actionItemRepository.findByIdAndOrganizationId(id, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Action item not found"));
    }

    private Map<UUID, User> usersById(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private ActionItemSummary toSummary(ActionItem item, Map<UUID, User> usersById) {
        User assignee = usersById.get(item.getAssigneeId());
        return toSummary(item, assignee);
    }

    private ActionItemSummary toSummary(ActionItem item, User assignee) {
        String assigneeName = assignee != null ? assignee.getFirstName() + " " + assignee.getLastName() : "Unknown";
        return new ActionItemSummary(item.getId(), item.getMeetingId(), item.getTitle(), item.getDescription(),
                item.getAssigneeId(), assigneeName, item.getDueDate(), item.getStatus(), item.getCreatedAt());
    }
}

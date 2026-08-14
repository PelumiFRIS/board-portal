package com.fris.boardportal.actionitem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "action_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActionItem {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "assignee_id", nullable = false)
    private UUID assigneeId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionItemStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ActionItem create(UUID organizationId, UUID meetingId, String title, String description,
            UUID assigneeId, LocalDate dueDate, UUID createdBy) {
        ActionItem item = new ActionItem();
        item.setId(UUID.randomUUID());
        item.setOrganizationId(organizationId);
        item.setMeetingId(meetingId);
        item.setTitle(title);
        item.setDescription(description);
        item.setAssigneeId(assigneeId);
        item.setDueDate(dueDate);
        item.setStatus(ActionItemStatus.OPEN);
        item.setCreatedBy(createdBy);
        Instant now = Instant.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }
}

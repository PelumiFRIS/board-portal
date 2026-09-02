package com.fris.boardportal.meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column
    private String location;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "scheduled_end")
    private Instant scheduledEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status;

    @Column(name = "minutes_content")
    private String minutesContent;

    @Column(name = "committee_id")
    private UUID committeeId;

    @Column(name = "meeting_type_id", nullable = false)
    private UUID meetingTypeId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Meeting create(UUID organizationId, UUID createdBy, String title, String description,
            String location, Instant scheduledStart, Instant scheduledEnd, UUID committeeId,
            UUID meetingTypeId) {
        Meeting meeting = new Meeting();
        meeting.setId(UUID.randomUUID());
        meeting.setOrganizationId(organizationId);
        meeting.setCreatedBy(createdBy);
        meeting.setTitle(title);
        meeting.setDescription(description);
        meeting.setLocation(location);
        meeting.setScheduledStart(scheduledStart);
        meeting.setScheduledEnd(scheduledEnd);
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setCommitteeId(committeeId);
        meeting.setMeetingTypeId(meetingTypeId);
        Instant now = Instant.now();
        meeting.setCreatedAt(now);
        meeting.setUpdatedAt(now);
        return meeting;
    }
}

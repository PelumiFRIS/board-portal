package com.fris.boardportal.meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agenda_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgendaItem {

    @Id
    private UUID id;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AgendaItem create(UUID meetingId, int position, String title, String description) {
        AgendaItem item = new AgendaItem();
        item.setId(UUID.randomUUID());
        item.setMeetingId(meetingId);
        item.setPosition(position);
        item.setTitle(title);
        item.setDescription(description);
        Instant now = Instant.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }
}

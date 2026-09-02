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
@Table(name = "meeting_type_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingTypeOption {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static MeetingTypeOption create(UUID organizationId, String name) {
        MeetingTypeOption option = new MeetingTypeOption();
        option.setId(UUID.randomUUID());
        option.setOrganizationId(organizationId);
        option.setName(name);
        option.setCreatedAt(Instant.now());
        return option;
    }
}

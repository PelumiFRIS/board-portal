package com.fris.boardportal.messaging;

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
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "is_group", nullable = false)
    private boolean group;

    @Column
    private String title;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Conversation create(UUID organizationId, boolean group, String title, UUID createdBy) {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setOrganizationId(organizationId);
        conversation.setGroup(group);
        conversation.setTitle(title);
        conversation.setCreatedBy(createdBy);
        conversation.setCreatedAt(Instant.now());
        return conversation;
    }
}

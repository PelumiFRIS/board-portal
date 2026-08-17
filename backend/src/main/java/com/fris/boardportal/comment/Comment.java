package com.fris.boardportal.comment;

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
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private CommentEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Comment create(UUID organizationId, CommentEntityType entityType, UUID entityId, UUID authorId,
            String authorName, String body) {
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setOrganizationId(organizationId);
        comment.setEntityType(entityType);
        comment.setEntityId(entityId);
        comment.setAuthorId(authorId);
        comment.setAuthorName(authorName);
        comment.setBody(body);
        comment.setCreatedAt(Instant.now());
        return comment;
    }
}

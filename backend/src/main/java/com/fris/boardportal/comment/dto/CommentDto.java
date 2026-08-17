package com.fris.boardportal.comment.dto;

import com.fris.boardportal.comment.Comment;
import com.fris.boardportal.comment.CommentEntityType;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        UUID id,
        CommentEntityType entityType,
        UUID entityId,
        UUID authorId,
        String authorName,
        String body,
        Instant createdAt) {

    public static CommentDto from(Comment comment) {
        return new CommentDto(comment.getId(), comment.getEntityType(), comment.getEntityId(),
                comment.getAuthorId(), comment.getAuthorName(), comment.getBody(), comment.getCreatedAt());
    }
}

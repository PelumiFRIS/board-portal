package com.fris.boardportal.comment.dto;

import com.fris.boardportal.comment.CommentEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCommentRequest(
        @NotNull CommentEntityType entityType,
        @NotNull UUID entityId,
        @NotBlank String body) {
}

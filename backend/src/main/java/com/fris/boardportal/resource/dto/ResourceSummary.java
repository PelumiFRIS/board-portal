package com.fris.boardportal.resource.dto;

import com.fris.boardportal.resource.ResourceCategory;
import java.time.Instant;
import java.util.UUID;

public record ResourceSummary(
        UUID id,
        ResourceCategory category,
        String title,
        String body,
        String fileName,
        String contentType,
        Long fileSize,
        Instant createdAt,
        Instant updatedAt) {
}

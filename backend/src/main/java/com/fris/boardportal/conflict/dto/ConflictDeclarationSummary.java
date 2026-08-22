package com.fris.boardportal.conflict.dto;

import java.time.Instant;
import java.util.UUID;

public record ConflictDeclarationSummary(
        UUID id,
        UUID userId,
        String userName,
        boolean hasConflict,
        String details,
        Instant declaredAt,
        String declaredByName) {
}

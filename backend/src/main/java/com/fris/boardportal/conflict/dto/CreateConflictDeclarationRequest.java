package com.fris.boardportal.conflict.dto;

import java.util.UUID;

public record CreateConflictDeclarationRequest(UUID userId, boolean hasConflict, String details) {
}

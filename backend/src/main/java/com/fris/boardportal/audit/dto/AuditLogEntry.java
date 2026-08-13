package com.fris.boardportal.audit.dto;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogEntry(
        UUID id,
        AuditAction action,
        AuditEntityType entityType,
        UUID entityId,
        String summary,
        String actorName,
        Instant createdAt) {

    public static AuditLogEntry from(AuditLog log) {
        return new AuditLogEntry(log.getId(), log.getAction(), log.getEntityType(), log.getEntityId(),
                log.getSummary(), log.getActorName(), log.getCreatedAt());
    }
}

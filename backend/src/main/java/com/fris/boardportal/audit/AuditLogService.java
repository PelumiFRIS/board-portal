package com.fris.boardportal.audit;

import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(AppUserPrincipal actor, AuditAction action, AuditEntityType entityType, UUID entityId,
            String summary) {
        String actorName = userRepository.findById(actor.getUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(actor.getUsername());
        AuditLog log = AuditLog.create(actor.getOrganizationId(), actor.getUserId(), actorName, action, entityType,
                entityId, summary);
        auditLogRepository.save(log);
    }

    public List<AuditLogEntry> listForOrganization(AppUserPrincipal principal) {
        return auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.getOrganizationId()).stream()
                .map(AuditLogEntry::from)
                .toList();
    }
}

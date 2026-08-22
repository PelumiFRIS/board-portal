package com.fris.boardportal.audit;

import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.UserRepository;
import java.nio.charset.StandardCharsets;
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

    public byte[] exportCsv(AppUserPrincipal admin) {
        StringBuilder csv = new StringBuilder();
        csv.append("When,Who,Action,Entity Type,Summary\n");
        for (AuditLogEntry entry : listForOrganization(admin)) {
            csv.append(csvField(entry.createdAt().toString())).append(',')
                    .append(csvField(entry.actorName())).append(',')
                    .append(csvField(entry.action().toString())).append(',')
                    .append(csvField(entry.entityType().toString())).append(',')
                    .append(csvField(entry.summary())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

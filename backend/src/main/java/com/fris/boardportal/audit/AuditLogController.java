package com.fris.boardportal.audit;

import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.security.AppUserPrincipal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLogEntry> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return auditLogService.listForOrganization(principal);
    }
}

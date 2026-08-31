package com.fris.boardportal.audit;

import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.security.AppUserPrincipal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public List<AuditLogEntry> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return auditLogService.listForOrganization(principal);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AppUserPrincipal principal) {
        byte[] csv = auditLogService.exportCsv(principal);
        String filename = "audit-trail-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}

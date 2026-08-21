package com.fris.boardportal.compliance;

import com.fris.boardportal.compliance.dto.ComplianceFilingSummary;
import com.fris.boardportal.compliance.dto.CreateComplianceFilingRequest;
import com.fris.boardportal.compliance.dto.UpdateComplianceFilingRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compliance-filings")
public class ComplianceFilingController {

    private final ComplianceFilingService filingService;

    public ComplianceFilingController(ComplianceFilingService filingService) {
        this.filingService = filingService;
    }

    @GetMapping
    public List<ComplianceFilingSummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return filingService.listForOrganization(principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplianceFilingSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateComplianceFilingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(filingService.create(principal, request.title(), request.description(), request.dueDate()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ComplianceFilingSummary update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @RequestBody UpdateComplianceFilingRequest request) {
        return filingService.update(principal, id, request.title(), request.description(), request.dueDate());
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasRole('ADMIN')")
    public ComplianceFilingSummary markSubmitted(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        return filingService.markSubmitted(principal, id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        filingService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}

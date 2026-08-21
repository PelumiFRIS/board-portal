package com.fris.boardportal.compliance;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.compliance.dto.ComplianceFilingSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceFilingService {

    private final ComplianceFilingRepository filingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ComplianceFilingService(ComplianceFilingRepository filingRepository, UserRepository userRepository,
            AuditLogService auditLogService) {
        this.filingRepository = filingRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<ComplianceFilingSummary> listForOrganization(AppUserPrincipal principal) {
        return filingRepository.findByOrganizationIdOrderByDueDateAsc(principal.getOrganizationId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public ComplianceFilingSummary create(AppUserPrincipal admin, String title, String description,
            LocalDate dueDate) {
        ComplianceFiling filing = ComplianceFiling.create(admin.getOrganizationId(), title, description, dueDate,
                admin.getUserId());
        filingRepository.save(filing);

        auditLogService.record(admin, AuditAction.FILING_CREATED, AuditEntityType.COMPLIANCE_FILING, filing.getId(),
                "Created filing \"" + filing.getTitle() + "\" due " + filing.getDueDate());

        return toSummary(filing);
    }

    @Transactional
    public ComplianceFilingSummary update(AppUserPrincipal admin, UUID filingId, String title, String description,
            LocalDate dueDate) {
        ComplianceFiling filing = findInOrg(admin, filingId);
        if (title != null) {
            filing.setTitle(title);
        }
        if (description != null) {
            filing.setDescription(description);
        }
        if (dueDate != null) {
            filing.setDueDate(dueDate);
        }
        filing.setUpdatedAt(Instant.now());
        filingRepository.save(filing);

        auditLogService.record(admin, AuditAction.FILING_UPDATED, AuditEntityType.COMPLIANCE_FILING, filing.getId(),
                "Updated filing \"" + filing.getTitle() + "\"");

        return toSummary(filing);
    }

    @Transactional
    public ComplianceFilingSummary markSubmitted(AppUserPrincipal admin, UUID filingId) {
        ComplianceFiling filing = findInOrg(admin, filingId);
        filing.setStatus(ComplianceFilingStatus.SUBMITTED);
        filing.setSubmittedAt(Instant.now());
        filing.setSubmittedBy(admin.getUserId());
        filing.setUpdatedAt(Instant.now());
        filingRepository.save(filing);

        auditLogService.record(admin, AuditAction.FILING_SUBMITTED, AuditEntityType.COMPLIANCE_FILING, filing.getId(),
                "Marked filing \"" + filing.getTitle() + "\" as submitted");

        return toSummary(filing);
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID filingId) {
        ComplianceFiling filing = findInOrg(admin, filingId);
        filingRepository.delete(filing);

        auditLogService.record(admin, AuditAction.FILING_DELETED, AuditEntityType.COMPLIANCE_FILING, filing.getId(),
                "Deleted filing \"" + filing.getTitle() + "\"");
    }

    private ComplianceFiling findInOrg(AppUserPrincipal principal, UUID filingId) {
        return filingRepository.findByIdAndOrganizationId(filingId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Filing not found"));
    }

    private ComplianceFilingSummary toSummary(ComplianceFiling filing) {
        String submittedByName = null;
        if (filing.getSubmittedBy() != null) {
            User submitter = userRepository.findById(filing.getSubmittedBy()).orElse(null);
            if (submitter != null) {
                submittedByName = submitter.getFirstName() + " " + submitter.getLastName();
            }
        }
        return new ComplianceFilingSummary(filing.getId(), filing.getTitle(), filing.getDescription(),
                filing.getDueDate(), filing.getStatus(), filing.getSubmittedAt(), submittedByName,
                filing.getCreatedAt());
    }
}

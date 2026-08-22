package com.fris.boardportal.conflict;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.conflict.dto.ConflictDeclarationSummary;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConflictDeclarationService {

    private final ConflictDeclarationRepository declarationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ConflictDeclarationService(ConflictDeclarationRepository declarationRepository,
            UserRepository userRepository, AuditLogService auditLogService) {
        this.declarationRepository = declarationRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<ConflictDeclarationSummary> listForUser(AppUserPrincipal principal) {
        return declarationRepository.findByUserIdOrderByDeclaredAtDesc(principal.getUserId()).stream()
                .map(this::toSummary)
                .toList();
    }

    public List<ConflictDeclarationSummary> listForOrganization(AppUserPrincipal admin) {
        return declarationRepository.findByOrganizationIdOrderByDeclaredAtDesc(admin.getOrganizationId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public ConflictDeclarationSummary declare(AppUserPrincipal principal, UUID targetUserId, boolean hasConflict,
            String details) {
        UUID resolvedUserId = targetUserId != null ? targetUserId : principal.getUserId();
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        boolean isSelf = resolvedUserId.equals(principal.getUserId());
        if (!isAdmin && !isSelf) {
            throw ApiException.forbidden("You can only declare on your own behalf");
        }

        User target = userRepository.findByIdAndOrganizationId(resolvedUserId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        User actor = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        ConflictDeclaration declaration = ConflictDeclaration.create(principal.getOrganizationId(), target.getId(),
                target.getFirstName() + " " + target.getLastName(), actor.getId(),
                actor.getFirstName() + " " + actor.getLastName(), hasConflict, details);
        declarationRepository.save(declaration);

        String summary = isSelf
                ? (hasConflict ? "Declared a conflict of interest" : "Declared no conflict of interest")
                : "Recorded a conflict of interest declaration for " + target.getFirstName() + " " + target.getLastName();
        auditLogService.record(principal, AuditAction.CONFLICT_DECLARED, AuditEntityType.CONFLICT_DECLARATION,
                declaration.getId(), summary);

        return toSummary(declaration);
    }

    private ConflictDeclarationSummary toSummary(ConflictDeclaration declaration) {
        return new ConflictDeclarationSummary(declaration.getId(), declaration.getUserId(), declaration.getUserName(),
                declaration.isHasConflict(), declaration.getDetails(), declaration.getDeclaredAt(),
                declaration.getDeclaredByName());
    }
}

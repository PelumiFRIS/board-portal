package com.fris.boardportal.conflict;

import com.fris.boardportal.conflict.dto.ConflictDeclarationSummary;
import com.fris.boardportal.conflict.dto.CreateConflictDeclarationRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conflict-declarations")
public class ConflictDeclarationController {

    private final ConflictDeclarationService declarationService;

    public ConflictDeclarationController(ConflictDeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @GetMapping("/me")
    public List<ConflictDeclarationSummary> listMine(@AuthenticationPrincipal AppUserPrincipal principal) {
        return declarationService.listForUser(principal);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConflictDeclarationSummary> listAll(@AuthenticationPrincipal AppUserPrincipal principal) {
        return declarationService.listForOrganization(principal);
    }

    @PostMapping
    public ResponseEntity<ConflictDeclarationSummary> declare(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateConflictDeclarationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(declarationService.declare(principal, request.userId(), request.hasConflict(), request.details()));
    }
}

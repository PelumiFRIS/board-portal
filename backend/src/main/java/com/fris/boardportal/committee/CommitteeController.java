package com.fris.boardportal.committee;

import com.fris.boardportal.committee.dto.AddCommitteeMemberRequest;
import com.fris.boardportal.committee.dto.CommitteeSummary;
import com.fris.boardportal.committee.dto.CreateCommitteeRequest;
import com.fris.boardportal.committee.dto.UpdateCommitteeRequest;
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
@RequestMapping("/api/committees")
public class CommitteeController {

    private final CommitteeService committeeService;

    public CommitteeController(CommitteeService committeeService) {
        this.committeeService = committeeService;
    }

    @GetMapping
    public List<CommitteeSummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return committeeService.listForOrganization(principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommitteeSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateCommitteeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(committeeService.create(principal, request.name(), request.description()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CommitteeSummary update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @RequestBody UpdateCommitteeRequest request) {
        return committeeService.update(principal, id, request.name(), request.description());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        committeeService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public CommitteeSummary addMember(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody AddCommitteeMemberRequest request) {
        return committeeService.addMember(principal, id, request.userId());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CommitteeSummary removeMember(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @PathVariable UUID userId) {
        return committeeService.removeMember(principal, id, userId);
    }

    @PatchMapping("/{id}/members/{userId}/chair")
    @PreAuthorize("hasRole('ADMIN')")
    public CommitteeSummary setChair(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @PathVariable UUID userId) {
        return committeeService.setChair(principal, id, userId);
    }
}

package com.fris.boardportal.actionitem;

import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.actionitem.dto.CreateActionItemRequest;
import com.fris.boardportal.actionitem.dto.UpdateActionItemStatusRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/action-items")
public class ActionItemController {

    private final ActionItemService actionItemService;

    public ActionItemController(ActionItemService actionItemService) {
        this.actionItemService = actionItemService;
    }

    @GetMapping
    public List<ActionItemSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) UUID meetingId) {
        return actionItemService.listForOrganization(principal, meetingId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ActionItemSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateActionItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actionItemService.create(principal, request));
    }

    @PatchMapping("/{id}/status")
    public ActionItemSummary updateStatus(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @Valid @RequestBody UpdateActionItemStatusRequest request) {
        return actionItemService.updateStatus(principal, id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        actionItemService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}

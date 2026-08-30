package com.fris.boardportal.resource;

import com.fris.boardportal.resource.dto.CreateResourceRequest;
import com.fris.boardportal.resource.dto.ResourceSummary;
import com.fris.boardportal.resource.dto.UpdateResourceRequest;
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
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<ResourceSummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return resourceService.listForOrganization(principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.create(principal, request.category(), request.title(), request.body()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResourceSummary update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @RequestBody UpdateResourceRequest request) {
        return resourceService.update(principal, id, request.category(), request.title(), request.body());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        resourceService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}

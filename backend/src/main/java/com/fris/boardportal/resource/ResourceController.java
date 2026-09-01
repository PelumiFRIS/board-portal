package com.fris.boardportal.resource;

import com.fris.boardportal.resource.dto.ResourceSummary;
import com.fris.boardportal.resource.dto.UpdateResourceRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

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
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public ResponseEntity<ResourceSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam ResourceCategory category, @RequestParam String title, @RequestParam String body,
            @RequestParam(required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.create(principal, category, title, body, file));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        Resource resource = resourceService.getContent(principal, id);
        String encodedName = URLEncoder.encode(resource.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resource.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource.getFileData());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public ResourceSummary update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @RequestBody UpdateResourceRequest request) {
        return resourceService.update(principal, id, request.category(), request.title(), request.body());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        resourceService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}

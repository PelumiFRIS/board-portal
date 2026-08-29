package com.fris.boardportal.document;

import com.fris.boardportal.document.dto.DocumentDetail;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.document.dto.UpdateDocumentRetentionRequest;
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
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) UUID meetingId,
            @RequestParam(required = false) DocumentCategory category,
            @RequestParam(required = false) UUID committeeId) {
        return documentService.listForOrganization(principal, meetingId, category, committeeId);
    }

    @GetMapping("/{id}")
    public DocumentDetail detail(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return documentService.getDetail(principal, id);
    }

    @GetMapping("/{id}/versions")
    public List<DocumentSummary> versions(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return documentService.listVersions(principal, id);
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentSummary> uploadVersion(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        DocumentSummary created = documentService.uploadNewVersion(principal, id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        Document document = documentService.getContent(principal, id);
        String encodedName = URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(document.getFileData());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentSummary> upload(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam DocumentCategory category,
            @RequestParam(required = false) UUID meetingId,
            @RequestParam(required = false) UUID committeeId) {
        DocumentSummary created = documentService.upload(principal, file, title, description, category, meetingId,
                committeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        documentService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/retention")
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentSummary updateRetention(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @RequestBody UpdateDocumentRetentionRequest request) {
        return documentService.updateRetention(principal, id, request.retentionUntil());
    }

    @PostMapping("/{id}/sign")
    public DocumentSummary sign(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return documentService.sign(principal, id);
    }
}

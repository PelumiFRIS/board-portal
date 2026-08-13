package com.fris.boardportal.document;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.meeting.MeetingRepository;
import com.fris.boardportal.security.AppUserPrincipal;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MeetingRepository meetingRepository;
    private final AuditLogService auditLogService;

    public DocumentService(DocumentRepository documentRepository, MeetingRepository meetingRepository,
            AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.meetingRepository = meetingRepository;
        this.auditLogService = auditLogService;
    }

    public List<DocumentSummary> listForOrganization(AppUserPrincipal principal, UUID meetingId,
            DocumentCategory category) {
        return documentRepository.findSummariesByOrganizationId(principal.getOrganizationId()).stream()
                .filter(d -> meetingId == null || meetingId.equals(d.meetingId()))
                .filter(d -> category == null || category == d.category())
                .toList();
    }

    public DocumentSummary getSummary(AppUserPrincipal principal, UUID id) {
        return toSummary(findInOrg(principal, id));
    }

    public Document getContent(AppUserPrincipal principal, UUID id) {
        return findInOrg(principal, id);
    }

    @Transactional
    public DocumentSummary upload(AppUserPrincipal admin, MultipartFile file, String title, String description,
            DocumentCategory category, UUID meetingId) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("A file is required");
        }
        if (meetingId != null) {
            meetingRepository.findByIdAndOrganizationId(meetingId, admin.getOrganizationId())
                    .orElseThrow(() -> ApiException.notFound("Meeting not found"));
        }

        byte[] fileData;
        try {
            fileData = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }

        Document document = Document.create(
                admin.getOrganizationId(),
                meetingId,
                title,
                description,
                category,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document",
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                fileData,
                admin.getUserId());
        documentRepository.save(document);

        auditLogService.record(admin, AuditAction.DOCUMENT_UPLOADED, AuditEntityType.DOCUMENT, document.getId(),
                "Uploaded \"" + document.getTitle() + "\" (" + document.getCategory() + ")");

        return toSummary(document);
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID id) {
        Document document = findInOrg(admin, id);
        documentRepository.delete(document);

        auditLogService.record(admin, AuditAction.DOCUMENT_DELETED, AuditEntityType.DOCUMENT, document.getId(),
                "Deleted \"" + document.getTitle() + "\"");
    }

    private Document findInOrg(AppUserPrincipal principal, UUID id) {
        return documentRepository.findByIdAndOrganizationId(id, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Document not found"));
    }

    private DocumentSummary toSummary(Document d) {
        return new DocumentSummary(d.getId(), d.getTitle(), d.getDescription(), d.getCategory(), d.getFileName(),
                d.getContentType(), d.getFileSize(), d.getMeetingId(), d.getCreatedAt());
    }
}

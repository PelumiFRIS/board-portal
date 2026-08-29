package com.fris.boardportal.document;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.committee.Committee;
import com.fris.boardportal.committee.CommitteeRepository;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.document.dto.DocumentDetail;
import com.fris.boardportal.document.dto.DocumentSignatureDto;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.meeting.MeetingRepository;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentSignatureRepository signatureRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final CommitteeRepository committeeRepository;

    public DocumentService(DocumentRepository documentRepository, DocumentSignatureRepository signatureRepository,
            MeetingRepository meetingRepository, UserRepository userRepository, AuditLogService auditLogService,
            CommitteeRepository committeeRepository) {
        this.documentRepository = documentRepository;
        this.signatureRepository = signatureRepository;
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.committeeRepository = committeeRepository;
    }

    public List<DocumentSummary> listForOrganization(AppUserPrincipal principal, UUID meetingId,
            DocumentCategory category, UUID committeeId) {
        return documentRepository.findSummariesByOrganizationId(principal.getOrganizationId()).stream()
                .filter(d -> meetingId == null || meetingId.equals(d.meetingId()))
                .filter(d -> category == null || category == d.category())
                .filter(d -> committeeId == null || committeeId.equals(d.committeeId()))
                .map(d -> withSignatureInfo(d, principal.getUserId()))
                .toList();
    }

    public DocumentDetail getDetail(AppUserPrincipal principal, UUID id) {
        Document document = findInOrg(principal, id);
        List<DocumentSignatureDto> signatures = signatureRepository.findByDocumentIdOrderBySignedAtAsc(document.getId())
                .stream()
                .map(s -> new DocumentSignatureDto(s.getUserId(), s.getUserName(), s.getSignedAt()))
                .toList();
        boolean signedByMe = signatures.stream().anyMatch(s -> s.userId().equals(principal.getUserId()));
        return new DocumentDetail(document.getId(), document.getTitle(), document.getDescription(),
                document.getCategory(), document.getFileName(), document.getContentType(), document.getFileSize(),
                document.getMeetingId(), document.getCommitteeId(), document.getCreatedAt(),
                document.getRetentionUntil(), signatures.size(), signedByMe, signatures);
    }

    public Document getContent(AppUserPrincipal principal, UUID id) {
        Document document = findInOrg(principal, id);
        auditLogService.record(principal, AuditAction.DOCUMENT_DOWNLOADED, AuditEntityType.DOCUMENT, document.getId(),
                "Downloaded \"" + document.getTitle() + "\"");
        return document;
    }

    @Transactional
    public DocumentSummary upload(AppUserPrincipal admin, MultipartFile file, String title, String description,
            DocumentCategory category, UUID meetingId, UUID committeeId) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("A file is required");
        }
        if (meetingId != null) {
            meetingRepository.findByIdAndOrganizationId(meetingId, admin.getOrganizationId())
                    .orElseThrow(() -> ApiException.notFound("Meeting not found"));
        }
        Committee committee = null;
        if (committeeId != null) {
            committee = committeeRepository.findByIdAndOrganizationId(committeeId, admin.getOrganizationId())
                    .orElseThrow(() -> ApiException.notFound("Committee not found"));
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
                admin.getUserId(),
                committeeId);
        documentRepository.save(document);

        String summary = committee != null
                ? "Uploaded \"" + document.getTitle() + "\" (" + document.getCategory() + ") for committee \""
                        + committee.getName() + "\""
                : "Uploaded \"" + document.getTitle() + "\" (" + document.getCategory() + ")";
        auditLogService.record(admin, AuditAction.DOCUMENT_UPLOADED, AuditEntityType.DOCUMENT, document.getId(),
                summary);

        return toSummary(document, 0, false);
    }

    @Transactional
    public void delete(AppUserPrincipal admin, UUID id) {
        Document document = findInOrg(admin, id);
        documentRepository.delete(document);

        auditLogService.record(admin, AuditAction.DOCUMENT_DELETED, AuditEntityType.DOCUMENT, document.getId(),
                "Deleted \"" + document.getTitle() + "\"");
    }

    @Transactional
    public DocumentSummary updateRetention(AppUserPrincipal admin, UUID id, LocalDate retentionUntil) {
        Document document = findInOrg(admin, id);
        document.setRetentionUntil(retentionUntil);
        documentRepository.save(document);

        String summary = retentionUntil != null
                ? "Set retention date for \"" + document.getTitle() + "\" to " + retentionUntil
                : "Cleared retention date for \"" + document.getTitle() + "\"";
        auditLogService.record(admin, AuditAction.DOCUMENT_RETENTION_SET, AuditEntityType.DOCUMENT, document.getId(),
                summary);

        return withSignatureInfo(toSummary(document, 0, false), admin.getUserId());
    }

    @Transactional
    public DocumentSummary sign(AppUserPrincipal principal, UUID id) {
        Document document = findInOrg(principal, id);
        if (!signatureRepository.existsByDocumentIdAndUserId(document.getId(), principal.getUserId())) {
            User signer = userRepository.findById(principal.getUserId())
                    .orElseThrow(() -> ApiException.notFound("User not found"));
            signatureRepository.save(DocumentSignature.create(document.getId(), document.getOrganizationId(),
                    signer.getId(), signer.getFirstName() + " " + signer.getLastName()));
            auditLogService.record(principal, AuditAction.DOCUMENT_SIGNED, AuditEntityType.DOCUMENT, document.getId(),
                    "Signed off on \"" + document.getTitle() + "\"");
        }
        return withSignatureInfo(toSummary(document, 0, false), principal.getUserId());
    }

    private Document findInOrg(AppUserPrincipal principal, UUID id) {
        return documentRepository.findByIdAndOrganizationId(id, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Document not found"));
    }

    private DocumentSummary withSignatureInfo(DocumentSummary summary, UUID userId) {
        long count = signatureRepository.countByDocumentId(summary.id());
        boolean signedByMe = signatureRepository.existsByDocumentIdAndUserId(summary.id(), userId);
        return new DocumentSummary(summary.id(), summary.title(), summary.description(), summary.category(),
                summary.fileName(), summary.contentType(), summary.fileSize(), summary.meetingId(),
                summary.committeeId(), summary.createdAt(), summary.retentionUntil(), count, signedByMe);
    }

    private DocumentSummary toSummary(Document d, long signatureCount, boolean signedByMe) {
        return new DocumentSummary(d.getId(), d.getTitle(), d.getDescription(), d.getCategory(), d.getFileName(),
                d.getContentType(), d.getFileSize(), d.getMeetingId(), d.getCommitteeId(), d.getCreatedAt(),
                d.getRetentionUntil(), signatureCount, signedByMe);
    }
}

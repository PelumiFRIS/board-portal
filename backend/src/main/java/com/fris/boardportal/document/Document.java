package com.fris.boardportal.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "meeting_id")
    private UUID meetingId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentCategory category;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    @Column(name = "committee_id")
    private UUID committeeId;

    @Column(name = "root_document_id", nullable = false)
    private UUID rootDocumentId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    public static Document create(UUID organizationId, UUID meetingId, String title, String description,
            DocumentCategory category, String fileName, String contentType, byte[] fileData, UUID uploadedBy,
            UUID committeeId, UUID rootDocumentId, int versionNumber) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setOrganizationId(organizationId);
        document.setMeetingId(meetingId);
        document.setTitle(title);
        document.setDescription(description);
        document.setCategory(category);
        document.setFileName(fileName);
        document.setContentType(contentType);
        document.setFileSize(fileData.length);
        document.setFileData(fileData);
        document.setUploadedBy(uploadedBy);
        document.setCreatedAt(Instant.now());
        document.setCommitteeId(committeeId);
        document.setRootDocumentId(rootDocumentId != null ? rootDocumentId : document.getId());
        document.setVersionNumber(versionNumber);
        return document;
    }
}

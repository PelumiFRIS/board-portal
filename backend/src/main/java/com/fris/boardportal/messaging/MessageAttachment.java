package com.fris.boardportal.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "message_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {

    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    public static MessageAttachment create(UUID messageId, String fileName, String contentType, byte[] fileData) {
        MessageAttachment attachment = new MessageAttachment();
        attachment.setId(UUID.randomUUID());
        attachment.setMessageId(messageId);
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setFileSize(fileData.length);
        attachment.setFileData(fileData);
        attachment.setUploadedAt(Instant.now());
        return attachment;
    }
}

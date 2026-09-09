package com.fris.boardportal.messaging.dto;

import com.fris.boardportal.messaging.MessageAttachment;
import java.util.UUID;

public record AttachmentSummary(UUID id, String fileName, String contentType, long fileSize) {

    public static AttachmentSummary from(MessageAttachment attachment) {
        return new AttachmentSummary(attachment.getId(), attachment.getFileName(), attachment.getContentType(),
                attachment.getFileSize());
    }
}

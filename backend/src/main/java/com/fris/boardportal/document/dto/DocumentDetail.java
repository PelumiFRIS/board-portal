package com.fris.boardportal.document.dto;

import com.fris.boardportal.document.DocumentCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DocumentDetail(
        UUID id,
        String title,
        String description,
        DocumentCategory category,
        String fileName,
        String contentType,
        long fileSize,
        UUID meetingId,
        UUID committeeId,
        Instant createdAt,
        LocalDate retentionUntil,
        long signatureCount,
        boolean signedByMe,
        List<DocumentSignatureDto> signatures) {
}

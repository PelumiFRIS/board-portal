package com.fris.boardportal.document.dto;

import java.time.LocalDate;

public record UpdateDocumentRetentionRequest(LocalDate retentionUntil) {
}

package com.fris.boardportal.document.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentSignatureDto(UUID userId, String userName, Instant signedAt) {
}

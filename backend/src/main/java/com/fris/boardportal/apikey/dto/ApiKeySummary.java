package com.fris.boardportal.apikey.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiKeySummary(
        UUID id,
        String name,
        String keyPrefix,
        Instant createdAt,
        Instant lastUsedAt) {
}

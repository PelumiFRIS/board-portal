package com.fris.boardportal.compliance.dto;

import com.fris.boardportal.compliance.ComplianceFilingStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ComplianceFilingSummary(
        UUID id,
        String title,
        String description,
        LocalDate dueDate,
        ComplianceFilingStatus status,
        Instant submittedAt,
        String submittedByName,
        Instant createdAt) {
}

package com.fris.boardportal.compliance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateComplianceFilingRequest(@NotBlank String title, String description, @NotNull LocalDate dueDate) {
}

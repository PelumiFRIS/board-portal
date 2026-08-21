package com.fris.boardportal.compliance.dto;

import java.time.LocalDate;

public record UpdateComplianceFilingRequest(String title, String description, LocalDate dueDate) {
}

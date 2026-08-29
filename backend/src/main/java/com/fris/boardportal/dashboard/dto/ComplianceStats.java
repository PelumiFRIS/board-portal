package com.fris.boardportal.dashboard.dto;

public record ComplianceStats(long total, long submitted, long pending, long overdue, double complianceRate) {
}

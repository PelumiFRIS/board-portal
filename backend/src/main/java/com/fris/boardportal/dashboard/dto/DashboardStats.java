package com.fris.boardportal.dashboard.dto;

public record DashboardStats(
        MeetingStats meetings,
        ResolutionStats resolutions,
        ActionItemStats actionItems,
        ComplianceStats compliance) {
}

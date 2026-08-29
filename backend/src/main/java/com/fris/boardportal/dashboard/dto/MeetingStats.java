package com.fris.boardportal.dashboard.dto;

import java.util.List;

public record MeetingStats(long total, long scheduled, long completed, long cancelled, List<MonthlyCount> cadence) {
}

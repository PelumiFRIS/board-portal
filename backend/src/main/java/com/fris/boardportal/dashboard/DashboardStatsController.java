package com.fris.boardportal.dashboard;

import com.fris.boardportal.dashboard.dto.DashboardStats;
import com.fris.boardportal.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    public DashboardStatsController(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = dashboardStatsService;
    }

    @GetMapping("/stats")
    public DashboardStats stats(@AuthenticationPrincipal AppUserPrincipal principal) {
        return dashboardStatsService.getStats(principal);
    }
}

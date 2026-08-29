package com.fris.boardportal.dashboard;

import com.fris.boardportal.actionitem.ActionItem;
import com.fris.boardportal.actionitem.ActionItemRepository;
import com.fris.boardportal.actionitem.ActionItemStatus;
import com.fris.boardportal.compliance.ComplianceFiling;
import com.fris.boardportal.compliance.ComplianceFilingRepository;
import com.fris.boardportal.compliance.ComplianceFilingStatus;
import com.fris.boardportal.dashboard.dto.ActionItemStats;
import com.fris.boardportal.dashboard.dto.ComplianceStats;
import com.fris.boardportal.dashboard.dto.DashboardStats;
import com.fris.boardportal.dashboard.dto.MeetingStats;
import com.fris.boardportal.dashboard.dto.MonthlyCount;
import com.fris.boardportal.dashboard.dto.ResolutionStats;
import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.meeting.MeetingRepository;
import com.fris.boardportal.meeting.MeetingStatus;
import com.fris.boardportal.resolution.Resolution;
import com.fris.boardportal.resolution.ResolutionOutcome;
import com.fris.boardportal.resolution.ResolutionRepository;
import com.fris.boardportal.resolution.ResolutionStatus;
import com.fris.boardportal.security.AppUserPrincipal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardStatsService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

    private final MeetingRepository meetingRepository;
    private final ResolutionRepository resolutionRepository;
    private final ActionItemRepository actionItemRepository;
    private final ComplianceFilingRepository complianceFilingRepository;

    public DashboardStatsService(MeetingRepository meetingRepository, ResolutionRepository resolutionRepository,
            ActionItemRepository actionItemRepository, ComplianceFilingRepository complianceFilingRepository) {
        this.meetingRepository = meetingRepository;
        this.resolutionRepository = resolutionRepository;
        this.actionItemRepository = actionItemRepository;
        this.complianceFilingRepository = complianceFilingRepository;
    }

    public DashboardStats getStats(AppUserPrincipal principal) {
        return new DashboardStats(
                meetingStats(principal),
                resolutionStats(principal),
                actionItemStats(principal),
                complianceStats(principal));
    }

    private MeetingStats meetingStats(AppUserPrincipal principal) {
        List<Meeting> meetings = meetingRepository.findByOrganizationIdOrderByScheduledStartDesc(
                principal.getOrganizationId());

        long scheduled = meetings.stream().filter(m -> m.getStatus() == MeetingStatus.SCHEDULED).count();
        long completed = meetings.stream().filter(m -> m.getStatus() == MeetingStatus.COMPLETED).count();
        long cancelled = meetings.stream().filter(m -> m.getStatus() == MeetingStatus.CANCELLED).count();

        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        List<MonthlyCount> cadence = java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(i -> currentMonth.minusMonths(5 - i))
                .map(month -> {
                    long count = meetings.stream()
                            .filter(m -> YearMonth.from(m.getScheduledStart().atZone(ZoneOffset.UTC)).equals(month))
                            .count();
                    return new MonthlyCount(month.format(MONTH_LABEL), count);
                })
                .toList();

        return new MeetingStats(meetings.size(), scheduled, completed, cancelled, cadence);
    }

    private ResolutionStats resolutionStats(AppUserPrincipal principal) {
        List<Resolution> resolutions = resolutionRepository.findByOrganizationIdOrderByCreatedAtDesc(
                principal.getOrganizationId());

        long open = resolutions.stream().filter(r -> r.getStatus() == ResolutionStatus.OPEN).count();
        long closed = resolutions.stream().filter(r -> r.getStatus() == ResolutionStatus.CLOSED).count();
        long passed = resolutions.stream().filter(r -> r.getOutcome() == ResolutionOutcome.PASSED).count();
        long failed = resolutions.stream().filter(r -> r.getOutcome() == ResolutionOutcome.FAILED).count();
        double passRate = closed == 0 ? 0 : (double) passed / closed;

        return new ResolutionStats(resolutions.size(), open, closed, passed, failed, passRate);
    }

    private ActionItemStats actionItemStats(AppUserPrincipal principal) {
        List<ActionItem> items = actionItemRepository.findByOrganizationIdOrderByCreatedAtDesc(
                principal.getOrganizationId());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        long open = items.stream().filter(i -> i.getStatus() == ActionItemStatus.OPEN).count();
        long done = items.stream().filter(i -> i.getStatus() == ActionItemStatus.DONE).count();
        long overdue = items.stream()
                .filter(i -> i.getStatus() == ActionItemStatus.OPEN)
                .filter(i -> i.getDueDate() != null && i.getDueDate().isBefore(today))
                .count();

        return new ActionItemStats(items.size(), open, done, overdue);
    }

    private ComplianceStats complianceStats(AppUserPrincipal principal) {
        List<ComplianceFiling> filings = complianceFilingRepository.findByOrganizationIdOrderByDueDateAsc(
                principal.getOrganizationId());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        long submitted = filings.stream().filter(f -> f.getStatus() == ComplianceFilingStatus.SUBMITTED).count();
        long pending = filings.stream().filter(f -> f.getStatus() == ComplianceFilingStatus.PENDING).count();
        long overdue = filings.stream()
                .filter(f -> f.getStatus() == ComplianceFilingStatus.PENDING)
                .filter(f -> f.getDueDate().isBefore(today))
                .count();
        double complianceRate = filings.isEmpty() ? 0 : (double) submitted / filings.size();

        return new ComplianceStats(filings.size(), submitted, pending, overdue, complianceRate);
    }
}

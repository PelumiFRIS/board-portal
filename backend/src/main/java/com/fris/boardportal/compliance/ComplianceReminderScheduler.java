package com.fris.boardportal.compliance;

import com.fris.boardportal.notification.EmailNotificationService;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import com.fris.boardportal.user.UserStatus;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ComplianceReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ComplianceReminderScheduler.class);
    private static final List<Integer> REMINDER_THRESHOLDS_DAYS = List.of(7, 1);

    private final ComplianceFilingRepository filingRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public ComplianceReminderScheduler(ComplianceFilingRepository filingRepository, UserRepository userRepository,
            EmailNotificationService emailNotificationService) {
        this.filingRepository = filingRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    public void sendDueSoonReminders() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<UUID, List<User>> activeMembersByOrg = new HashMap<>();

        for (int daysUntilDue : REMINDER_THRESHOLDS_DAYS) {
            LocalDate targetDueDate = today.plusDays(daysUntilDue);
            List<ComplianceFiling> dueSoon =
                    filingRepository.findByStatusAndDueDate(ComplianceFilingStatus.PENDING, targetDueDate);

            for (ComplianceFiling filing : dueSoon) {
                List<User> recipients = activeMembersByOrg.computeIfAbsent(filing.getOrganizationId(),
                        orgId -> userRepository.findByOrganizationId(orgId).stream()
                                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                                .toList());
                emailNotificationService.notifyFilingDueSoon(filing, recipients, daysUntilDue);
            }
            log.info("Compliance reminder pass: {} filing(s) due in {} day(s)", dueSoon.size(), daysUntilDue);
        }
    }
}

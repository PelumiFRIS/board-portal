package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.compliance.ComplianceReminderScheduler;
import com.fris.boardportal.compliance.dto.ComplianceFilingSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The scheduler scans every org's filings on each run (not just the one this test just
 * created), and the test database is not rolled back between methods, so filings left
 * behind by earlier tests in this class can still be PENDING and due at a threshold.
 * Titles are made unique per test and assertions filter captured messages down to the
 * ones for that title, rather than asserting a total invocation count across the run.
 */
class ComplianceReminderSchedulerTest extends IntegrationTestSupport {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private ComplianceReminderScheduler scheduler;

    @Test
    void sendsReminderForFilingDueInSevenDays() {
        AuthResponse admin = signup(uniqueEmail(), "Filing Reminder 7d Org");
        String title = uniqueTitle("Annual Return");
        createFiling(admin.accessToken(), title, LocalDate.now(ZoneOffset.UTC).plusDays(7));

        scheduler.sendDueSoonReminders();

        List<SimpleMailMessage> matching = messagesForTitle(title);
        assertThat(matching).hasSize(1);
        assertThat(matching.get(0).getSubject()).contains("due in 7 days");
        assertThat(matching.get(0).getBcc()).contains(admin.user().email());
    }

    @Test
    void sendsUrgentReminderForFilingDueTomorrow() {
        AuthResponse admin = signup(uniqueEmail(), "Filing Reminder 1d Org");
        String title = uniqueTitle("Tax Filing");
        createFiling(admin.accessToken(), title, LocalDate.now(ZoneOffset.UTC).plusDays(1));

        scheduler.sendDueSoonReminders();

        List<SimpleMailMessage> matching = messagesForTitle(title);
        assertThat(matching).hasSize(1);
        assertThat(matching.get(0).getSubject()).isEqualTo("Filing due tomorrow: " + title);
    }

    @Test
    void sendsSeparateRemindersForFilingsAtDifferentThresholds() {
        AuthResponse admin = signup(uniqueEmail(), "Filing Reminder Multi Org");
        String sevenDayTitle = uniqueTitle("Seven Day Filing");
        String oneDayTitle = uniqueTitle("One Day Filing");
        createFiling(admin.accessToken(), sevenDayTitle, LocalDate.now(ZoneOffset.UTC).plusDays(7));
        createFiling(admin.accessToken(), oneDayTitle, LocalDate.now(ZoneOffset.UTC).plusDays(1));

        scheduler.sendDueSoonReminders();

        assertThat(messagesForTitle(sevenDayTitle)).hasSize(1);
        assertThat(messagesForTitle(oneDayTitle)).hasSize(1);
        assertThat(messagesForTitle(sevenDayTitle).get(0).getSubject()).contains("due in 7 days");
        assertThat(messagesForTitle(oneDayTitle).get(0).getSubject()).contains("due tomorrow");
    }

    @Test
    void skipsSubmittedFilingsEvenAtThresholdDueDate() {
        AuthResponse admin = signup(uniqueEmail(), "Filing Reminder Submitted Org");
        String title = uniqueTitle("Already Done");
        ComplianceFilingSummary filing = createFiling(admin.accessToken(), title, LocalDate.now(ZoneOffset.UTC).plusDays(7));
        ResponseEntity<ComplianceFilingSummary> submitted = restTemplate.exchange(
                "/api/compliance-filings/" + filing.id() + "/submit", HttpMethod.PATCH,
                authedRequest(admin.accessToken()), ComplianceFilingSummary.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);

        scheduler.sendDueSoonReminders();

        assertThat(messagesForTitle(title)).isEmpty();
    }

    @Test
    void skipsFilingsOffThreshold() {
        AuthResponse admin = signup(uniqueEmail(), "Filing Reminder Off Threshold Org");
        String threeDayTitle = uniqueTitle("Due In Three Days");
        String tenDayTitle = uniqueTitle("Due In Ten Days");
        createFiling(admin.accessToken(), threeDayTitle, LocalDate.now(ZoneOffset.UTC).plusDays(3));
        createFiling(admin.accessToken(), tenDayTitle, LocalDate.now(ZoneOffset.UTC).plusDays(10));

        scheduler.sendDueSoonReminders();

        assertThat(messagesForTitle(threeDayTitle)).isEmpty();
        assertThat(messagesForTitle(tenDayTitle)).isEmpty();
    }

    @Test
    void emailsAllActiveOrgMembersNotJustAdmin() {
        AuthResponse admin = signup(uniqueEmail(), "Filing Reminder Recipients Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        String title = uniqueTitle("Org Wide Filing");
        createFiling(admin.accessToken(), title, LocalDate.now(ZoneOffset.UTC).plusDays(7));

        scheduler.sendDueSoonReminders();

        List<SimpleMailMessage> matching = messagesForTitle(title);
        assertThat(matching).hasSize(1);
        assertThat(matching.get(0).getBcc()).contains(admin.user().email(), memberEmail);
    }

    @Test
    void reminderSurvivesMailSenderFailure() {
        AuthResponse admin = signup(uniqueEmail(), "Filing Reminder Failure Org");
        createFiling(admin.accessToken(), uniqueTitle("Failure Filing"), LocalDate.now(ZoneOffset.UTC).plusDays(7));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> scheduler.sendDueSoonReminders()).doesNotThrowAnyException();
    }

    private List<SimpleMailMessage> messagesForTitle(String title) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(captor.capture());
        return captor.getAllValues().stream()
                .filter(m -> m.getSubject() != null && m.getSubject().contains(title))
                .toList();
    }

    private String uniqueTitle(String prefix) {
        return prefix + " " + UUID.randomUUID();
    }

    private ComplianceFilingSummary createFiling(String adminToken, String title, LocalDate dueDate) {
        ResponseEntity<ComplianceFilingSummary> response = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.POST,
                authedRequest(adminToken, new CreateFilingRequest(title, null, dueDate)),
                ComplianceFilingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private UserSummary createBoardMember(String adminToken, String email) {
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private record CreateFilingRequest(String title, String description, LocalDate dueDate) {
    }
}

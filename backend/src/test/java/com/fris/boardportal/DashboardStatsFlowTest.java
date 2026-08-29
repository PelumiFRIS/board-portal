package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.fris.boardportal.actionitem.ActionItemStatus;
import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.actionitem.dto.CreateActionItemRequest;
import com.fris.boardportal.actionitem.dto.UpdateActionItemStatusRequest;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.compliance.dto.ComplianceFilingSummary;
import com.fris.boardportal.compliance.dto.CreateComplianceFilingRequest;
import com.fris.boardportal.dashboard.dto.DashboardStats;
import com.fris.boardportal.meeting.MeetingStatus;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.meeting.dto.UpdateMeetingRequest;
import com.fris.boardportal.resolution.VoteChoice;
import com.fris.boardportal.resolution.dto.CastVoteRequest;
import com.fris.boardportal.resolution.dto.CreateResolutionRequest;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class DashboardStatsFlowTest extends IntegrationTestSupport {

    @Test
    void emptyOrgReturnsAllZeroStatsWithoutError() {
        AuthResponse admin = signup(uniqueEmail(), "Empty Dashboard Org");

        ResponseEntity<DashboardStats> response = restTemplate.exchange(
                "/api/dashboard/stats", HttpMethod.GET, authedRequest(admin.accessToken()), DashboardStats.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DashboardStats stats = response.getBody();
        assertThat(stats.meetings().total()).isZero();
        assertThat(stats.meetings().cadence()).hasSize(6);
        assertThat(stats.meetings().cadence()).allSatisfy(m -> assertThat(m.count()).isZero());
        assertThat(stats.resolutions().total()).isZero();
        assertThat(stats.resolutions().passRate()).isZero();
        assertThat(stats.actionItems().total()).isZero();
        assertThat(stats.compliance().total()).isZero();
        assertThat(stats.compliance().complianceRate()).isZero();
    }

    @Test
    void statsReflectExactCountsAndRatesAcrossAllCategories() {
        AuthResponse admin = signup(uniqueEmail(), "Dashboard Stats Org");
        String memberEmail = uniqueEmail();
        UUID memberId = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        // meetings: 1 scheduled, 1 completed, 1 cancelled
        MeetingSummary m1 = scheduleMeeting(admin.accessToken(), "Scheduled Meeting");
        MeetingSummary m2 = scheduleMeeting(admin.accessToken(), "Completed Meeting");
        updateMeetingStatus(admin.accessToken(), m2.id(), MeetingStatus.COMPLETED);
        MeetingSummary m3 = scheduleMeeting(admin.accessToken(), "Cancelled Meeting");
        updateMeetingStatus(admin.accessToken(), m3.id(), MeetingStatus.CANCELLED);

        // resolutions: 1 draft, 1 open, 1 closed-passed, 1 closed-failed
        createResolution(admin.accessToken(), m1.id(), "Draft Resolution");
        ResolutionSummary open = createResolution(admin.accessToken(), m1.id(), "Open Resolution");
        openResolution(admin.accessToken(), open.id());
        ResolutionSummary willPass = createResolution(admin.accessToken(), m1.id(), "Passing Resolution");
        openResolution(admin.accessToken(), willPass.id());
        castVote(admin.accessToken(), willPass.id(), VoteChoice.FOR);
        closeResolution(admin.accessToken(), willPass.id());
        ResolutionSummary willFail = createResolution(admin.accessToken(), m1.id(), "Failing Resolution");
        openResolution(admin.accessToken(), willFail.id());
        castVote(admin.accessToken(), willFail.id(), VoteChoice.AGAINST);
        closeResolution(admin.accessToken(), willFail.id());

        // action items: 1 open-not-overdue, 1 open-overdue, 1 open-no-due-date, 1 done
        createActionItem(admin.accessToken(), m1.id(), memberId, "Not overdue", LocalDate.now().plusDays(7));
        createActionItem(admin.accessToken(), m1.id(), memberId, "Overdue", LocalDate.now().minusDays(3));
        createActionItem(admin.accessToken(), m1.id(), memberId, "No due date", null);
        ActionItemSummary willBeDone = createActionItem(admin.accessToken(), m1.id(), memberId, "Done", null);
        updateActionItemStatus(admin.accessToken(), willBeDone.id(), ActionItemStatus.DONE);

        // compliance filings: 1 pending-not-overdue, 1 pending-overdue, 1 submitted
        createFiling(admin.accessToken(), "Future Filing", LocalDate.now().plusDays(30));
        createFiling(admin.accessToken(), "Overdue Filing", LocalDate.now().minusDays(5));
        ComplianceFilingSummary willSubmit = createFiling(admin.accessToken(), "Submitted Filing", LocalDate.now().plusDays(1));
        submitFiling(admin.accessToken(), willSubmit.id());

        ResponseEntity<DashboardStats> response = restTemplate.exchange(
                "/api/dashboard/stats", HttpMethod.GET, authedRequest(member.accessToken()), DashboardStats.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DashboardStats stats = response.getBody();

        assertThat(stats.meetings().total()).isEqualTo(3);
        assertThat(stats.meetings().scheduled()).isEqualTo(1);
        assertThat(stats.meetings().completed()).isEqualTo(1);
        assertThat(stats.meetings().cancelled()).isEqualTo(1);
        assertThat(stats.meetings().cadence()).hasSize(6);
        assertThat(stats.meetings().cadence().get(5).count()).isEqualTo(3);

        assertThat(stats.resolutions().total()).isEqualTo(4);
        assertThat(stats.resolutions().open()).isEqualTo(1);
        assertThat(stats.resolutions().closed()).isEqualTo(2);
        assertThat(stats.resolutions().passed()).isEqualTo(1);
        assertThat(stats.resolutions().failed()).isEqualTo(1);
        assertThat(stats.resolutions().passRate()).isCloseTo(0.5, offset(0.0001));

        assertThat(stats.actionItems().total()).isEqualTo(4);
        assertThat(stats.actionItems().open()).isEqualTo(3);
        assertThat(stats.actionItems().done()).isEqualTo(1);
        assertThat(stats.actionItems().overdue()).isEqualTo(1);

        assertThat(stats.compliance().total()).isEqualTo(3);
        assertThat(stats.compliance().submitted()).isEqualTo(1);
        assertThat(stats.compliance().pending()).isEqualTo(2);
        assertThat(stats.compliance().overdue()).isEqualTo(1);
        assertThat(stats.compliance().complianceRate()).isCloseTo(1.0 / 3, offset(0.0001));
    }

    private MeetingSummary scheduleMeeting(String adminToken, String title) {
        Instant start = Instant.now();
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest(title, null, null, start, null, null, null)),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void updateMeetingStatus(String adminToken, UUID meetingId, MeetingStatus status) {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/meetings/" + meetingId, HttpMethod.PATCH,
                authedRequest(adminToken, new UpdateMeetingRequest(null, null, null, null, null, status, null, null, null)),
                Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResolutionSummary createResolution(String adminToken, UUID meetingId, String title) {
        ResponseEntity<ResolutionSummary> response = restTemplate.exchange(
                "/api/resolutions", HttpMethod.POST,
                authedRequest(adminToken, new CreateResolutionRequest(meetingId, title, null)), ResolutionSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void openResolution(String adminToken, UUID resolutionId) {
        ResponseEntity<ResolutionSummary> response = restTemplate.exchange(
                "/api/resolutions/" + resolutionId + "/open", HttpMethod.PATCH,
                authedRequest(adminToken), ResolutionSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void castVote(String token, UUID resolutionId, VoteChoice choice) {
        ResponseEntity<ResolutionSummary> response = restTemplate.exchange(
                "/api/resolutions/" + resolutionId + "/votes", HttpMethod.POST,
                authedRequest(token, new CastVoteRequest(choice)), ResolutionSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void closeResolution(String adminToken, UUID resolutionId) {
        ResponseEntity<ResolutionSummary> response = restTemplate.exchange(
                "/api/resolutions/" + resolutionId + "/close", HttpMethod.PATCH,
                authedRequest(adminToken), ResolutionSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ActionItemSummary createActionItem(String adminToken, UUID meetingId, UUID assigneeId, String title,
            LocalDate dueDate) {
        ResponseEntity<ActionItemSummary> response = restTemplate.exchange(
                "/api/action-items", HttpMethod.POST,
                authedRequest(adminToken, new CreateActionItemRequest(meetingId, title, null, assigneeId, dueDate)),
                ActionItemSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void updateActionItemStatus(String adminToken, UUID itemId, ActionItemStatus status) {
        ResponseEntity<ActionItemSummary> response = restTemplate.exchange(
                "/api/action-items/" + itemId + "/status", HttpMethod.PATCH,
                authedRequest(adminToken, new UpdateActionItemStatusRequest(status)), ActionItemSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ComplianceFilingSummary createFiling(String adminToken, String title, LocalDate dueDate) {
        ResponseEntity<ComplianceFilingSummary> response = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.POST,
                authedRequest(adminToken, new CreateComplianceFilingRequest(title, null, dueDate)),
                ComplianceFilingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void submitFiling(String adminToken, UUID filingId) {
        ResponseEntity<ComplianceFilingSummary> response = restTemplate.exchange(
                "/api/compliance-filings/" + filingId + "/submit", HttpMethod.PATCH,
                authedRequest(adminToken), ComplianceFilingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private UUID createBoardMember(String adminToken, String email) {
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new com.fris.boardportal.auth.dto.LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}

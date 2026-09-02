package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.resolution.ResolutionOutcome;
import com.fris.boardportal.resolution.ResolutionStatus;
import com.fris.boardportal.resolution.VoteChoice;
import com.fris.boardportal.resolution.dto.CastVoteRequest;
import com.fris.boardportal.resolution.dto.CreateResolutionRequest;
import com.fris.boardportal.resolution.dto.ResolutionDetail;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ResolutionFlowTest extends IntegrationTestSupport {

    @Test
    void nonAdminCannotCreateOpenOrCloseResolutions() {
        AuthResponse admin = signup(uniqueEmail(), "Restricted Resolutions Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/resolutions", HttpMethod.POST,
                authedRequest(member.accessToken(), new CreateResolutionRequest(meeting.id(), "Approve budget", null)),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResolutionSummary resolution = createResolution(admin.accessToken(), meeting.id());

        ResponseEntity<String> openResponse = restTemplate.exchange(
                "/api/resolutions/" + resolution.id() + "/open", HttpMethod.PATCH,
                authedRequest(member.accessToken()), String.class);
        assertThat(openResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void fullLifecycleWithMultipleVotersAndOutcomePassed() {
        AuthResponse admin = signup(uniqueEmail(), "Lifecycle Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        ResolutionSummary created = createResolution(admin.accessToken(), meeting.id());
        assertThat(created.status()).isEqualTo(ResolutionStatus.DRAFT);

        String memberAEmail = uniqueEmail();
        String memberBEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberAEmail);
        createBoardMember(admin.accessToken(), memberBEmail);
        AuthResponse memberA = login(memberAEmail);
        AuthResponse memberB = login(memberBEmail);

        // voting not open yet
        ResponseEntity<String> voteBeforeOpen = castVoteRaw(admin.accessToken(), created.id(), VoteChoice.FOR);
        assertThat(voteBeforeOpen.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<ResolutionSummary> opened = restTemplate.exchange(
                "/api/resolutions/" + created.id() + "/open", HttpMethod.PATCH,
                authedRequest(admin.accessToken()), ResolutionSummary.class);
        assertThat(opened.getBody().status()).isEqualTo(ResolutionStatus.OPEN);

        // admin votes FOR, then changes mind to AGAINST (re-cast should update, not duplicate)
        castVote(admin.accessToken(), created.id(), VoteChoice.FOR);
        ResolutionSummary afterChange = castVote(admin.accessToken(), created.id(), VoteChoice.AGAINST);
        assertThat(afterChange.forCount()).isZero();
        assertThat(afterChange.againstCount()).isEqualTo(1);

        // now flip admin back to FOR, and both members vote FOR too -> 3 FOR vs 0 AGAINST
        castVote(admin.accessToken(), created.id(), VoteChoice.FOR);
        castVote(memberA.accessToken(), created.id(), VoteChoice.FOR);
        castVote(memberB.accessToken(), created.id(), VoteChoice.ABSTAIN);

        ResponseEntity<ResolutionDetail> detailResponse = restTemplate.exchange(
                "/api/resolutions/" + created.id(), HttpMethod.GET,
                authedRequest(memberA.accessToken()), ResolutionDetail.class);
        ResolutionDetail detail = detailResponse.getBody();
        assertThat(detail.forCount()).isEqualTo(2);
        assertThat(detail.abstainCount()).isEqualTo(1);
        assertThat(detail.votes()).hasSize(3);

        ResponseEntity<ResolutionSummary> closed = restTemplate.exchange(
                "/api/resolutions/" + created.id() + "/close", HttpMethod.PATCH,
                authedRequest(admin.accessToken()), ResolutionSummary.class);
        assertThat(closed.getBody().status()).isEqualTo(ResolutionStatus.CLOSED);
        assertThat(closed.getBody().outcome()).isEqualTo(ResolutionOutcome.PASSED);

        // voting rejected once closed
        ResponseEntity<String> voteAfterClose = castVoteRaw(memberA.accessToken(), created.id(), VoteChoice.AGAINST);
        assertThat(voteAfterClose.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // deleting a closed resolution is rejected
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/resolutions/" + created.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), String.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void tiedVoteFails() {
        AuthResponse admin = signup(uniqueEmail(), "Tie Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        ResolutionSummary created = createResolution(admin.accessToken(), meeting.id());
        restTemplate.exchange("/api/resolutions/" + created.id() + "/open", HttpMethod.PATCH,
                authedRequest(admin.accessToken()), ResolutionSummary.class);

        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        castVote(admin.accessToken(), created.id(), VoteChoice.FOR);
        castVote(member.accessToken(), created.id(), VoteChoice.AGAINST);

        ResponseEntity<ResolutionSummary> closed = restTemplate.exchange(
                "/api/resolutions/" + created.id() + "/close", HttpMethod.PATCH,
                authedRequest(admin.accessToken()), ResolutionSummary.class);
        assertThat(closed.getBody().outcome()).isEqualTo(ResolutionOutcome.FAILED);
    }

    @Test
    void adminCanExportResolutionsCsvButNonAdminCannot() {
        AuthResponse admin = signup(uniqueEmail(), "Export Resolutions Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        createResolution(admin.accessToken(), meeting.id());

        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/resolutions/export", HttpMethod.GET, authedRequest(member.accessToken()), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> exported = restTemplate.exchange(
                "/api/resolutions/export", HttpMethod.GET, authedRequest(admin.accessToken()), String.class);
        assertThat(exported.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exported.getHeaders().getContentType()).isNotNull();
        assertThat(exported.getHeaders().getContentType().toString()).contains("text/csv");
        assertThat(exported.getBody()).contains("Meeting,Title,Status,Outcome,For,Against,Abstain,Opened At,Closed At");
        assertThat(exported.getBody()).contains("Q3 Board Meeting");
        assertThat(exported.getBody()).contains("Approve FY26 budget");
    }

    @Test
    void exportingResolutionsForOrgWithNoneReturnsHeaderOnlyCsv() {
        AuthResponse admin = signup(uniqueEmail(), "Empty Export Resolutions Org");

        ResponseEntity<String> exported = restTemplate.exchange(
                "/api/resolutions/export", HttpMethod.GET, authedRequest(admin.accessToken()), String.class);
        assertThat(exported.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exported.getBody()).isEqualTo(
                "Meeting,Title,Status,Outcome,For,Against,Abstain,Opened At,Closed At\n");
    }

    @Test
    void adminCannotAccessResolutionFromAnotherOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Resolutions Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Resolutions Org B");
        MeetingSummary orgBMeeting = scheduleMeeting(orgBAdmin.accessToken());
        ResolutionSummary orgBResolution = createResolution(orgBAdmin.accessToken(), orgBMeeting.id());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/resolutions/" + orgBResolution.id(), HttpMethod.GET,
                authedRequest(orgAAdmin.accessToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private MeetingSummary scheduleMeeting(String adminToken) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest("Q3 Board Meeting", "Quarterly review", "Virtual",
                        start, start.plus(1, ChronoUnit.HOURS), null, defaultMeetingTypeId(adminToken))),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResolutionSummary createResolution(String adminToken, java.util.UUID meetingId) {
        ResponseEntity<ResolutionSummary> response = restTemplate.exchange(
                "/api/resolutions", HttpMethod.POST,
                authedRequest(adminToken, new CreateResolutionRequest(meetingId, "Approve FY26 budget",
                        "Resolved that the FY26 budget as presented is approved.")),
                ResolutionSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResolutionSummary castVote(String token, java.util.UUID resolutionId, VoteChoice choice) {
        ResponseEntity<ResolutionSummary> response = castVoteRaw(token, resolutionId, choice, ResolutionSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<String> castVoteRaw(String token, java.util.UUID resolutionId, VoteChoice choice) {
        return castVoteRaw(token, resolutionId, choice, String.class);
    }

    private <T> ResponseEntity<T> castVoteRaw(String token, java.util.UUID resolutionId, VoteChoice choice,
            Class<T> responseType) {
        return restTemplate.exchange(
                "/api/resolutions/" + resolutionId + "/votes", HttpMethod.POST,
                authedRequest(token, new CastVoteRequest(choice)), responseType);
    }

    private void createBoardMember(String adminToken, String email) {
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
    }

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}

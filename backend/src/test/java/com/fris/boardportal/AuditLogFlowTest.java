package com.fris.boardportal;
import com.fris.boardportal.meeting.MeetingType;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.resolution.VoteChoice;
import com.fris.boardportal.resolution.dto.CastVoteRequest;
import com.fris.boardportal.resolution.dto.CreateResolutionRequest;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class AuditLogFlowTest extends IntegrationTestSupport {

    @Test
    void nonAdminCannotViewAuditLog() {
        AuthResponse admin = signup(uniqueEmail(), "Restricted Audit Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/audit-logs", HttpMethod.GET, authedRequest(member.accessToken()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCannotSeeAnotherOrganizationsAuditLog() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Audit Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Audit Org B");

        List<AuditLogEntry> orgAEntries = fetchAuditLog(orgAAdmin.accessToken());
        assertThat(orgAEntries).allSatisfy(e -> assertThat(e.summary()).doesNotContain("Audit Org B"));

        List<AuditLogEntry> orgBEntries = fetchAuditLog(orgBAdmin.accessToken());
        assertThat(orgBEntries).allSatisfy(e -> assertThat(e.summary()).doesNotContain("Audit Org A"));
    }

    @Test
    void actionsAcrossTheAppAreRecordedNewestFirst() {
        String adminEmail = uniqueEmail();
        AuthResponse admin = signup(adminEmail, "Full Flow Audit Org");

        List<AuditLogEntry> afterSignup = fetchAuditLog(admin.accessToken());
        assertThat(afterSignup).extracting(AuditLogEntry::action).contains(AuditAction.ORGANIZATION_SIGNUP);

        AuthResponse loggedInAgain = login(adminEmail);
        List<AuditLogEntry> afterLogin = fetchAuditLog(loggedInAgain.accessToken());
        assertThat(afterLogin).extracting(AuditLogEntry::action).contains(AuditAction.LOGIN);

        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        ResolutionSummary resolution = createResolution(admin.accessToken(), meeting.id());
        openResolution(admin.accessToken(), resolution.id());
        castVote(admin.accessToken(), resolution.id());
        closeResolution(admin.accessToken(), resolution.id());
        uploadDocument(admin.accessToken());

        List<AuditLogEntry> entries = fetchAuditLog(admin.accessToken());
        List<AuditAction> actions = entries.stream().map(AuditLogEntry::action).toList();

        assertThat(actions).contains(
                AuditAction.ORGANIZATION_SIGNUP,
                AuditAction.LOGIN,
                AuditAction.MEETING_CREATED,
                AuditAction.RESOLUTION_CREATED,
                AuditAction.RESOLUTION_OPENED,
                AuditAction.VOTE_CAST,
                AuditAction.RESOLUTION_CLOSED,
                AuditAction.DOCUMENT_UPLOADED);

        // newest-first ordering
        for (int i = 0; i < entries.size() - 1; i++) {
            assertThat(entries.get(i).createdAt()).isAfterOrEqualTo(entries.get(i + 1).createdAt());
        }
    }

    @Test
    void adminCanExportCsvButNonAdminCannot() {
        AuthResponse admin = signup(uniqueEmail(), "Export Audit Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/audit-logs/export", HttpMethod.GET, authedRequest(member.accessToken()), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> exported = restTemplate.exchange(
                "/api/audit-logs/export", HttpMethod.GET, authedRequest(admin.accessToken()), String.class);
        assertThat(exported.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exported.getHeaders().getContentType()).isNotNull();
        assertThat(exported.getHeaders().getContentType().toString()).contains("text/csv");
        assertThat(exported.getBody()).contains("When,Who,Action,Entity Type,Summary");
        assertThat(exported.getBody()).contains("Created organization \"\"Export Audit Org\"\" and admin account");
    }

    private List<AuditLogEntry> fetchAuditLog(String adminToken) {
        ResponseEntity<AuditLogEntry[]> response = restTemplate.exchange(
                "/api/audit-logs", HttpMethod.GET, authedRequest(adminToken), AuditLogEntry[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return List.of(response.getBody());
    }

    private MeetingSummary scheduleMeeting(String adminToken) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest("Audit Test Meeting", null, null, start, null, null, MeetingType.BOARD)),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResolutionSummary createResolution(String adminToken, java.util.UUID meetingId) {
        ResponseEntity<ResolutionSummary> response = restTemplate.exchange(
                "/api/resolutions", HttpMethod.POST,
                authedRequest(adminToken, new CreateResolutionRequest(meetingId, "Audit Test Resolution", null)),
                ResolutionSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void openResolution(String adminToken, java.util.UUID resolutionId) {
        restTemplate.exchange("/api/resolutions/" + resolutionId + "/open", HttpMethod.PATCH,
                authedRequest(adminToken), ResolutionSummary.class);
    }

    private void closeResolution(String adminToken, java.util.UUID resolutionId) {
        restTemplate.exchange("/api/resolutions/" + resolutionId + "/close", HttpMethod.PATCH,
                authedRequest(adminToken), ResolutionSummary.class);
    }

    private void castVote(String adminToken, java.util.UUID resolutionId) {
        restTemplate.exchange("/api/resolutions/" + resolutionId + "/votes", HttpMethod.POST,
                authedRequest(adminToken, new CastVoteRequest(VoteChoice.FOR)), ResolutionSummary.class);
    }

    private void uploadDocument(String adminToken) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource("hello".getBytes()) {
            @Override
            public String getFilename() {
                return "audit-test.txt";
            }
        });
        form.add("title", "Audit Test Doc");
        form.add("category", "OTHER");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, headers);

        restTemplate.exchange("/api/documents", HttpMethod.POST, entity, DocumentSummary.class);
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

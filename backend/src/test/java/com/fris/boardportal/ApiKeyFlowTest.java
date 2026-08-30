package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.apikey.dto.ApiKeySummary;
import com.fris.boardportal.apikey.dto.CreateApiKeyRequest;
import com.fris.boardportal.apikey.dto.CreateApiKeyResponse;
import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.UserStatus;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UpdateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiKeyFlowTest extends IntegrationTestSupport {

    @Test
    void nonAdminCannotManageApiKeys() {
        AuthResponse admin = signup(uniqueEmail(), "Api Key View Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<String> list = restTemplate.exchange(
                "/api/api-keys", HttpMethod.GET, authedRequest(memberAuth.accessToken()), String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> create = restTemplate.exchange(
                "/api/api-keys", HttpMethod.POST,
                authedRequest(memberAuth.accessToken(), new CreateApiKeyRequest("Sneaky Key")), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanCreateListAndRevokeAKey() {
        AuthResponse admin = signup(uniqueEmail(), "Api Key Admin Org");

        CreateApiKeyResponse created = createApiKey(admin.accessToken(), "Zapier Sync");
        assertThat(created.rawKey()).startsWith("bpk_");
        assertThat(created.key().keyPrefix()).isEqualTo(created.rawKey().substring(0, 12));

        ResponseEntity<ApiKeySummary[]> list = restTemplate.exchange(
                "/api/api-keys", HttpMethod.GET, authedRequest(admin.accessToken()), ApiKeySummary[].class);
        assertThat(list.getBody()).extracting(ApiKeySummary::name).containsExactly("Zapier Sync");

        ResponseEntity<Void> revoked = restTemplate.exchange(
                "/api/api-keys/" + created.key().id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ApiKeySummary[]> listAfter = restTemplate.exchange(
                "/api/api-keys", HttpMethod.GET, authedRequest(admin.accessToken()), ApiKeySummary[].class);
        assertThat(listAfter.getBody()).isEmpty();
    }

    @Test
    void rawKeyAuthenticatesAgainstAllFourPublicEndpointsScopedToItsOwnOrganization() {
        AuthResponse admin = signup(uniqueEmail(), "Public Api Org");
        scheduleMeeting(admin.accessToken(), "Q4 Board Meeting");
        CreateApiKeyResponse created = createApiKey(admin.accessToken(), "Integration Key");

        AuthResponse otherOrgAdmin = signup(uniqueEmail(), "Public Api Other Org");
        scheduleMeeting(otherOrgAdmin.accessToken(), "Other Org Meeting");

        ResponseEntity<MeetingSummary[]> meetings = restTemplate.exchange(
                "/api/v1/meetings", HttpMethod.GET, apiKeyRequest(created.rawKey()), MeetingSummary[].class);
        assertThat(meetings.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meetings.getBody()).extracting(MeetingSummary::title).containsExactly("Q4 Board Meeting");

        ResponseEntity<String> resolutions = restTemplate.exchange(
                "/api/v1/resolutions", HttpMethod.GET, apiKeyRequest(created.rawKey()), String.class);
        assertThat(resolutions.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> actionItems = restTemplate.exchange(
                "/api/v1/action-items", HttpMethod.GET, apiKeyRequest(created.rawKey()), String.class);
        assertThat(actionItems.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> documents = restTemplate.exchange(
                "/api/v1/documents", HttpMethod.GET, apiKeyRequest(created.rawKey()), String.class);
        assertThat(documents.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void invalidOrMissingKeyIsRejected() {
        ResponseEntity<String> noKey = restTemplate.exchange(
                "/api/v1/meetings", HttpMethod.GET, new org.springframework.http.HttpEntity<>(null, new HttpHeaders()),
                String.class);
        assertThat(noKey.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> garbageKey = restTemplate.exchange(
                "/api/v1/meetings", HttpMethod.GET, apiKeyRequest("bpk_not-a-real-key"), String.class);
        assertThat(garbageKey.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void revokedKeyStopsAuthenticating() {
        AuthResponse admin = signup(uniqueEmail(), "Api Key Revoke Org");
        CreateApiKeyResponse created = createApiKey(admin.accessToken(), "Temp Key");

        ResponseEntity<String> beforeRevoke = restTemplate.exchange(
                "/api/v1/meetings", HttpMethod.GET, apiKeyRequest(created.rawKey()), String.class);
        assertThat(beforeRevoke.getStatusCode()).isEqualTo(HttpStatus.OK);

        restTemplate.exchange(
                "/api/api-keys/" + created.key().id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);

        ResponseEntity<String> afterRevoke = restTemplate.exchange(
                "/api/v1/meetings", HttpMethod.GET, apiKeyRequest(created.rawKey()), String.class);
        assertThat(afterRevoke.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void disablingTheCreatingAdminAlsoInvalidatesTheirKey() {
        AuthResponse orgAdmin = signup(uniqueEmail(), "Api Key Disable Org");
        String secondAdminEmail = uniqueEmail();
        UserSummary secondAdmin = createAdmin(orgAdmin.accessToken(), secondAdminEmail);
        AuthResponse secondAdminAuth = login(secondAdminEmail);

        CreateApiKeyResponse created = createApiKey(secondAdminAuth.accessToken(), "Second Admin Key");

        ResponseEntity<String> beforeDisable = restTemplate.exchange(
                "/api/v1/meetings", HttpMethod.GET, apiKeyRequest(created.rawKey()), String.class);
        assertThat(beforeDisable.getStatusCode()).isEqualTo(HttpStatus.OK);

        restTemplate.exchange(
                "/api/users/" + secondAdmin.id(), HttpMethod.PATCH,
                authedRequest(orgAdmin.accessToken(), new UpdateUserRequest(null, UserStatus.DISABLED, null, null, null)),
                UserSummary.class);

        ResponseEntity<String> afterDisable = restTemplate.exchange(
                "/api/v1/meetings", HttpMethod.GET, apiKeyRequest(created.rawKey()), String.class);
        assertThat(afterDisable.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void auditLogRecordsCreateAndRevokeButNeverTheRawKey() {
        AuthResponse admin = signup(uniqueEmail(), "Api Key Audit Org");
        CreateApiKeyResponse created = createApiKey(admin.accessToken(), "Audited Key");
        restTemplate.exchange(
                "/api/api-keys/" + created.key().id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);

        ResponseEntity<AuditLogEntry[]> auditLog = restTemplate.exchange(
                "/api/audit-logs", HttpMethod.GET, authedRequest(admin.accessToken()), AuditLogEntry[].class);
        List<AuditLogEntry> entries = List.of(auditLog.getBody());

        assertThat(entries).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo(AuditAction.API_KEY_CREATED);
            assertThat(e.summary()).contains("Audited Key").doesNotContain(created.rawKey());
        });
        assertThat(entries).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo(AuditAction.API_KEY_REVOKED);
            assertThat(e.summary()).contains("Audited Key").doesNotContain(created.rawKey());
        });
    }

    private CreateApiKeyResponse createApiKey(String adminToken, String name) {
        ResponseEntity<CreateApiKeyResponse> response = restTemplate.exchange(
                "/api/api-keys", HttpMethod.POST, authedRequest(adminToken, new CreateApiKeyRequest(name)),
                CreateApiKeyResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private <T> org.springframework.http.HttpEntity<T> apiKeyRequest(String rawKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", rawKey);
        return new org.springframework.http.HttpEntity<>(null, headers);
    }

    private MeetingSummary scheduleMeeting(String adminToken, String title) {
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest(title, null, null, Instant.now(), null, null, null)),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private UserSummary createBoardMember(String adminToken, String email) {
        return createUser(adminToken, email, Role.BOARD_MEMBER);
    }

    private UserSummary createAdmin(String adminToken, String email) {
        return createUser(adminToken, email, Role.ADMIN);
    }

    private UserSummary createUser(String adminToken, String email, Role role) {
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", role)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}

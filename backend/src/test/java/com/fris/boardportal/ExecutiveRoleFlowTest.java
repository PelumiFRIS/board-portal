package com.fris.boardportal;
import com.fris.boardportal.meeting.MeetingType;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.actionitem.ActionItemStatus;
import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.actionitem.dto.CreateActionItemRequest;
import com.fris.boardportal.actionitem.dto.UpdateActionItemStatusRequest;
import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.committee.dto.CommitteeSummary;
import com.fris.boardportal.committee.dto.CreateCommitteeRequest;
import com.fris.boardportal.compliance.dto.ComplianceFilingSummary;
import com.fris.boardportal.compliance.dto.CreateComplianceFilingRequest;
import com.fris.boardportal.compliance.dto.UpdateComplianceFilingRequest;
import com.fris.boardportal.conflict.dto.ConflictDeclarationSummary;
import com.fris.boardportal.conflict.dto.CreateConflictDeclarationRequest;
import com.fris.boardportal.document.DocumentCategory;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.document.dto.UpdateDocumentRetentionRequest;
import com.fris.boardportal.meeting.dto.AgendaItemDto;
import com.fris.boardportal.meeting.dto.CreateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.resolution.dto.CreateResolutionRequest;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import com.fris.boardportal.resource.ResourceCategory;
import com.fris.boardportal.resource.dto.ResourceSummary;
import com.fris.boardportal.resource.dto.UpdateResourceRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
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

class ExecutiveRoleFlowTest extends IntegrationTestSupport {

    @Test
    void executiveCanScheduleMeetingsAndManageAgendaItems() {
        AuthResponse admin = signup(uniqueEmail(), "Executive Meetings Org");
        AuthResponse executive = createUserAndLogin(admin.accessToken(), Role.EXECUTIVE);

        ResponseEntity<MeetingSummary> created = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(executive.accessToken(),
                        new CreateMeetingRequest("Exec Scheduled Meeting", null, null, Instant.now(), null, null, MeetingType.BOARD)),
                MeetingSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AgendaItemDto> agendaItem = restTemplate.exchange(
                "/api/meetings/" + created.getBody().id() + "/agenda-items", HttpMethod.POST,
                authedRequest(executive.accessToken(), new CreateAgendaItemRequest("Opening remarks", null, null)),
                AgendaItemDto.class);
        assertThat(agendaItem.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void executiveCanUploadDocumentsAndSetRetention() {
        AuthResponse admin = signup(uniqueEmail(), "Executive Documents Org");
        AuthResponse executive = createUserAndLogin(admin.accessToken(), Role.EXECUTIVE);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("Exec upload".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "exec-report.txt";
            }
        });
        body.add("title", "Exec Uploaded Report");
        body.add("category", DocumentCategory.REPORT.name());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(executive.accessToken());

        ResponseEntity<DocumentSummary> uploaded = restTemplate.exchange(
                "/api/documents", HttpMethod.POST, new HttpEntity<>(body, headers), DocumentSummary.class);
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<DocumentSummary> retentionSet = restTemplate.exchange(
                "/api/documents/" + uploaded.getBody().id() + "/retention", HttpMethod.PATCH,
                authedRequest(executive.accessToken(),
                        new UpdateDocumentRetentionRequest(LocalDate.now().plusYears(1))),
                DocumentSummary.class);
        assertThat(retentionSet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retentionSet.getBody().retentionUntil()).isEqualTo(LocalDate.now().plusYears(1));
    }

    @Test
    void executiveCanCreateOpenAndCloseResolutions() {
        AuthResponse admin = signup(uniqueEmail(), "Executive Resolutions Org");
        AuthResponse executive = createUserAndLogin(admin.accessToken(), Role.EXECUTIVE);
        MeetingSummary meeting = scheduleMeeting(admin.accessToken(), "Resolutions Meeting");

        ResponseEntity<ResolutionSummary> created = restTemplate.exchange(
                "/api/resolutions", HttpMethod.POST,
                authedRequest(executive.accessToken(),
                        new CreateResolutionRequest(meeting.id(), "Approve the budget", null)),
                ResolutionSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ResolutionSummary> opened = restTemplate.exchange(
                "/api/resolutions/" + created.getBody().id() + "/open", HttpMethod.PATCH,
                authedRequest(executive.accessToken()), ResolutionSummary.class);
        assertThat(opened.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ResolutionSummary> closed = restTemplate.exchange(
                "/api/resolutions/" + created.getBody().id() + "/close", HttpMethod.PATCH,
                authedRequest(executive.accessToken()), ResolutionSummary.class);
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void executiveCanCreateActionItemsAndUpdateAnyonesStatus() {
        AuthResponse admin = signup(uniqueEmail(), "Executive Action Items Org");
        AuthResponse executive = createUserAndLogin(admin.accessToken(), Role.EXECUTIVE);
        String memberEmail = uniqueEmail();
        UserSummary member = createUser(admin.accessToken(), memberEmail, Role.BOARD_MEMBER);
        MeetingSummary meeting = scheduleMeeting(admin.accessToken(), "Action Items Meeting");

        ResponseEntity<ActionItemSummary> created = restTemplate.exchange(
                "/api/action-items", HttpMethod.POST,
                authedRequest(executive.accessToken(),
                        new CreateActionItemRequest(meeting.id(), "Follow up with auditors", null, member.id(), null)),
                ActionItemSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // executive updates an item assigned to someone else, not just their own
        ResponseEntity<ActionItemSummary> updated = restTemplate.exchange(
                "/api/action-items/" + created.getBody().id() + "/status", HttpMethod.PATCH,
                authedRequest(executive.accessToken(), new UpdateActionItemStatusRequest(ActionItemStatus.DONE)),
                ActionItemSummary.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().status()).isEqualTo(ActionItemStatus.DONE);
    }

    @Test
    void executiveCanManageComplianceFilings() {
        AuthResponse admin = signup(uniqueEmail(), "Executive Compliance Org");
        AuthResponse executive = createUserAndLogin(admin.accessToken(), Role.EXECUTIVE);

        ResponseEntity<ComplianceFilingSummary> created = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.POST,
                authedRequest(executive.accessToken(),
                        new CreateComplianceFilingRequest("Annual Return", null, LocalDate.now().plusDays(30))),
                ComplianceFilingSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ComplianceFilingSummary> updated = restTemplate.exchange(
                "/api/compliance-filings/" + created.getBody().id(), HttpMethod.PATCH,
                authedRequest(executive.accessToken(), new UpdateComplianceFilingRequest("Annual Return (Revised)", null, null)),
                ComplianceFilingSummary.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/compliance-filings/" + created.getBody().id(), HttpMethod.DELETE,
                authedRequest(executive.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void executiveCanManageResourcesAndCommittees() {
        AuthResponse admin = signup(uniqueEmail(), "Executive Resources Org");
        AuthResponse executive = createUserAndLogin(admin.accessToken(), Role.EXECUTIVE);

        ResponseEntity<ResourceSummary> resource = restTemplate.exchange(
                "/api/resources", HttpMethod.POST,
                resourceForm(executive.accessToken(), ResourceCategory.FAQ, "Exec FAQ", "Body"),
                ResourceSummary.class);
        assertThat(resource.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ResourceSummary> updatedResource = restTemplate.exchange(
                "/api/resources/" + resource.getBody().id(), HttpMethod.PATCH,
                authedRequest(executive.accessToken(), new UpdateResourceRequest(null, "Exec FAQ (Revised)", null)),
                ResourceSummary.class);
        assertThat(updatedResource.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> deletedResource = restTemplate.exchange(
                "/api/resources/" + resource.getBody().id(), HttpMethod.DELETE,
                authedRequest(executive.accessToken()), Void.class);
        assertThat(deletedResource.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<CommitteeSummary> committee = restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(executive.accessToken(), new CreateCommitteeRequest("Audit Committee", null, null)),
                CommitteeSummary.class);
        assertThat(committee.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void executiveCanDeclareConflictOnBehalfOfAnotherMemberAndViewAuditLog() {
        AuthResponse admin = signup(uniqueEmail(), "Executive Conflicts Org");
        AuthResponse executive = createUserAndLogin(admin.accessToken(), Role.EXECUTIVE);
        UserSummary member = createUser(admin.accessToken(), uniqueEmail(), Role.BOARD_MEMBER);

        ResponseEntity<ConflictDeclarationSummary> declared = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.POST,
                authedRequest(executive.accessToken(),
                        new CreateConflictDeclarationRequest(member.id(), true, "Has a family interest")),
                ConflictDeclarationSummary.class);
        assertThat(declared.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ConflictDeclarationSummary[]> all = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.GET, authedRequest(executive.accessToken()),
                ConflictDeclarationSummary[].class);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<AuditLogEntry[]> auditLog = restTemplate.exchange(
                "/api/audit-logs", HttpMethod.GET, authedRequest(executive.accessToken()), AuditLogEntry[].class);
        assertThat(auditLog.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void executiveCannotManageUsersOrApiKeysButBoardMemberCannotManageAnything() {
        AuthResponse admin = signup(uniqueEmail(), "Executive Boundary Org");
        AuthResponse executive = createUserAndLogin(admin.accessToken(), Role.EXECUTIVE);
        AuthResponse boardMember = createUserAndLogin(admin.accessToken(), Role.BOARD_MEMBER);

        ResponseEntity<String> executiveCreatesUser = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(executive.accessToken(),
                        new CreateUserRequest("Should", "Fail", uniqueEmail(), "password123", Role.BOARD_MEMBER)),
                String.class);
        assertThat(executiveCreatesUser.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> executiveCreatesApiKey = restTemplate.exchange(
                "/api/api-keys", HttpMethod.POST,
                authedRequest(executive.accessToken(), java.util.Map.of("name", "Should fail")),
                String.class);
        assertThat(executiveCreatesApiKey.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> boardMemberCreatesResource = restTemplate.exchange(
                "/api/resources", HttpMethod.POST,
                resourceForm(boardMember.accessToken(), ResourceCategory.OTHER, "Should fail", "Body"),
                String.class);
        assertThat(boardMemberCreatesResource.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpEntity<MultiValueMap<String, Object>> resourceForm(String token, ResourceCategory category,
            String title, String body) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("category", category.name());
        form.add("title", title);
        form.add("body", body);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return new HttpEntity<>(form, headers);
    }

    private MeetingSummary scheduleMeeting(String adminToken, String title) {
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest(title, null, null, Instant.now(), null, null, MeetingType.BOARD)),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private AuthResponse createUserAndLogin(String adminToken, Role role) {
        String email = uniqueEmail();
        createUser(adminToken, email, role);
        return login(email);
    }

    private UserSummary createUser(String adminToken, String email, Role role) {
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Test", "User", email, "password123", role)),
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

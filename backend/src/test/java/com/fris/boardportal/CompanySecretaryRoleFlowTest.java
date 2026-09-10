package com.fris.boardportal;

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
import com.fris.boardportal.organization.dto.OrganizationOnboardRequest;
import com.fris.boardportal.organization.dto.OrganizationOnboardResponse;
import com.fris.boardportal.resolution.dto.CreateResolutionRequest;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
import com.fris.boardportal.resource.ResourceCategory;
import com.fris.boardportal.resource.dto.ResourceSummary;
import com.fris.boardportal.resource.dto.UpdateResourceRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.PasswordResetResponse;
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

class CompanySecretaryRoleFlowTest extends IntegrationTestSupport {

    @Test
    void companySecretaryCanScheduleMeetingsAndManageAgendaItems() {
        AuthResponse admin = signup(uniqueEmail(), "Company Secretary Meetings Org");
        AuthResponse companySecretary = createUserAndLogin(admin.accessToken(), Role.COMPANY_SECRETARY);

        ResponseEntity<MeetingSummary> created = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(),
                        new CreateMeetingRequest("Board Scheduled Meeting", null, null, Instant.now(), null, null,
                                defaultMeetingTypeId(companySecretary.accessToken()))),
                MeetingSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AgendaItemDto> agendaItem = restTemplate.exchange(
                "/api/meetings/" + created.getBody().id() + "/agenda-items", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(), new CreateAgendaItemRequest("Opening remarks", null, null)),
                AgendaItemDto.class);
        assertThat(agendaItem.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void companySecretaryCanUploadDocumentsAndSetRetention() {
        AuthResponse admin = signup(uniqueEmail(), "Company Secretary Documents Org");
        AuthResponse companySecretary = createUserAndLogin(admin.accessToken(), Role.COMPANY_SECRETARY);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("Board upload".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "board-report.txt";
            }
        });
        body.add("title", "Board Uploaded Report");
        body.add("category", DocumentCategory.REPORT.name());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(companySecretary.accessToken());

        ResponseEntity<DocumentSummary> uploaded = restTemplate.exchange(
                "/api/documents", HttpMethod.POST, new HttpEntity<>(body, headers), DocumentSummary.class);
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<DocumentSummary> retentionSet = restTemplate.exchange(
                "/api/documents/" + uploaded.getBody().id() + "/retention", HttpMethod.PATCH,
                authedRequest(companySecretary.accessToken(),
                        new UpdateDocumentRetentionRequest(LocalDate.now().plusYears(1))),
                DocumentSummary.class);
        assertThat(retentionSet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retentionSet.getBody().retentionUntil()).isEqualTo(LocalDate.now().plusYears(1));
    }

    @Test
    void companySecretaryCanCreateOpenAndCloseResolutions() {
        AuthResponse admin = signup(uniqueEmail(), "Company Secretary Resolutions Org");
        AuthResponse companySecretary = createUserAndLogin(admin.accessToken(), Role.COMPANY_SECRETARY);
        MeetingSummary meeting = scheduleMeeting(companySecretary.accessToken(), "Resolutions Meeting");

        ResponseEntity<ResolutionSummary> created = restTemplate.exchange(
                "/api/resolutions", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(),
                        new CreateResolutionRequest(meeting.id(), "Approve the budget", null)),
                ResolutionSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ResolutionSummary> opened = restTemplate.exchange(
                "/api/resolutions/" + created.getBody().id() + "/open", HttpMethod.PATCH,
                authedRequest(companySecretary.accessToken()), ResolutionSummary.class);
        assertThat(opened.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ResolutionSummary> closed = restTemplate.exchange(
                "/api/resolutions/" + created.getBody().id() + "/close", HttpMethod.PATCH,
                authedRequest(companySecretary.accessToken()), ResolutionSummary.class);
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void companySecretaryCanCreateActionItemsAndUpdateAnyonesStatus() {
        AuthResponse admin = signup(uniqueEmail(), "Company Secretary Action Items Org");
        AuthResponse companySecretary = createUserAndLogin(admin.accessToken(), Role.COMPANY_SECRETARY);
        String memberEmail = uniqueEmail();
        UserSummary member = createUser(admin.accessToken(), memberEmail, Role.BOARD_MEMBER);
        MeetingSummary meeting = scheduleMeeting(companySecretary.accessToken(), "Action Items Meeting");

        ResponseEntity<ActionItemSummary> created = restTemplate.exchange(
                "/api/action-items", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(),
                        new CreateActionItemRequest(meeting.id(), "Follow up with auditors", null, member.id(), null)),
                ActionItemSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // company secretary updates an item assigned to someone else, not just their own
        ResponseEntity<ActionItemSummary> updated = restTemplate.exchange(
                "/api/action-items/" + created.getBody().id() + "/status", HttpMethod.PATCH,
                authedRequest(companySecretary.accessToken(), new UpdateActionItemStatusRequest(ActionItemStatus.DONE)),
                ActionItemSummary.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().status()).isEqualTo(ActionItemStatus.DONE);
    }

    @Test
    void companySecretaryCanManageComplianceFilings() {
        AuthResponse admin = signup(uniqueEmail(), "Company Secretary Compliance Org");
        AuthResponse companySecretary = createUserAndLogin(admin.accessToken(), Role.COMPANY_SECRETARY);

        ResponseEntity<ComplianceFilingSummary> created = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(),
                        new CreateComplianceFilingRequest("Annual Return", null, LocalDate.now().plusDays(30))),
                ComplianceFilingSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ComplianceFilingSummary> updated = restTemplate.exchange(
                "/api/compliance-filings/" + created.getBody().id(), HttpMethod.PATCH,
                authedRequest(companySecretary.accessToken(), new UpdateComplianceFilingRequest("Annual Return (Revised)", null, null)),
                ComplianceFilingSummary.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/compliance-filings/" + created.getBody().id(), HttpMethod.DELETE,
                authedRequest(companySecretary.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void companySecretaryCanManageResourcesAndCommittees() {
        AuthResponse admin = signup(uniqueEmail(), "Company Secretary Resources Org");
        AuthResponse companySecretary = createUserAndLogin(admin.accessToken(), Role.COMPANY_SECRETARY);

        ResponseEntity<ResourceSummary> resource = restTemplate.exchange(
                "/api/resources", HttpMethod.POST,
                resourceForm(companySecretary.accessToken(), ResourceCategory.FAQ, "Board FAQ", "Body"),
                ResourceSummary.class);
        assertThat(resource.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ResourceSummary> updatedResource = restTemplate.exchange(
                "/api/resources/" + resource.getBody().id(), HttpMethod.PATCH,
                authedRequest(companySecretary.accessToken(), new UpdateResourceRequest(null, "Board FAQ (Revised)", null)),
                ResourceSummary.class);
        assertThat(updatedResource.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> deletedResource = restTemplate.exchange(
                "/api/resources/" + resource.getBody().id(), HttpMethod.DELETE,
                authedRequest(companySecretary.accessToken()), Void.class);
        assertThat(deletedResource.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<CommitteeSummary> committee = restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(), new CreateCommitteeRequest("Audit Committee", null, null)),
                CommitteeSummary.class);
        assertThat(committee.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void companySecretaryCanDeclareConflictOnBehalfOfAnotherMemberAndViewAuditLog() {
        AuthResponse admin = signup(uniqueEmail(), "Company Secretary Conflicts Org");
        AuthResponse companySecretary = createUserAndLogin(admin.accessToken(), Role.COMPANY_SECRETARY);
        UserSummary member = createUser(admin.accessToken(), uniqueEmail(), Role.BOARD_MEMBER);

        ResponseEntity<ConflictDeclarationSummary> declared = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(),
                        new CreateConflictDeclarationRequest(member.id(), true, "Has a family interest")),
                ConflictDeclarationSummary.class);
        assertThat(declared.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ConflictDeclarationSummary[]> all = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.GET, authedRequest(companySecretary.accessToken()),
                ConflictDeclarationSummary[].class);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<AuditLogEntry[]> auditLog = restTemplate.exchange(
                "/api/audit-logs", HttpMethod.GET, authedRequest(companySecretary.accessToken()), AuditLogEntry[].class);
        assertThat(auditLog.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void companySecretaryCannotManageUsersOrApiKeysButBoardMemberCannotManageAnything() {
        AuthResponse admin = signup(uniqueEmail(), "Company Secretary Boundary Org");
        AuthResponse companySecretary = createUserAndLogin(admin.accessToken(), Role.COMPANY_SECRETARY);
        AuthResponse boardMember = createUserAndLogin(admin.accessToken(), Role.BOARD_MEMBER);

        ResponseEntity<String> companySecretaryCreatesUser = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(),
                        new CreateUserRequest("Should", "Fail", uniqueEmail(), "password123", Role.BOARD_MEMBER)),
                String.class);
        assertThat(companySecretaryCreatesUser.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> companySecretaryCreatesApiKey = restTemplate.exchange(
                "/api/api-keys", HttpMethod.POST,
                authedRequest(companySecretary.accessToken(), java.util.Map.of("name", "Should fail")),
                String.class);
        assertThat(companySecretaryCreatesApiKey.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> companySecretaryResetsPassword = restTemplate.exchange(
                "/api/users/" + companySecretary.user().id() + "/reset-password", HttpMethod.POST,
                authedRequest(companySecretary.accessToken()), String.class);
        assertThat(companySecretaryResetsPassword.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> boardMemberCreatesResource = restTemplate.exchange(
                "/api/resources", HttpMethod.POST,
                resourceForm(boardMember.accessToken(), ResourceCategory.OTHER, "Should fail", "Body"),
                String.class);
        assertThat(boardMemberCreatesResource.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminIsRejectedFromGovernanceContentEndpoints() {
        AuthResponse admin = signup(uniqueEmail(), "Admin Boundary Org");

        ResponseEntity<String> adminCreatesCommittee = restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateCommitteeRequest("Should fail", null, null)),
                String.class);
        assertThat(adminCreatesCommittee.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> adminCreatesResource = restTemplate.exchange(
                "/api/resources", HttpMethod.POST,
                resourceForm(admin.accessToken(), ResourceCategory.OTHER, "Should fail", "Body"),
                String.class);
        assertThat(adminCreatesResource.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> adminCreatesComplianceFiling = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateComplianceFilingRequest("Should fail", null, LocalDate.now().plusDays(30))),
                String.class);
        assertThat(adminCreatesComplianceFiling.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanResetAnyUsersPasswordAcrossOrganizationsAndTheUserCanLogInWithIt() {
        AuthResponse fris = signup(uniqueEmail(), "FRIS Ops Org");
        AuthResponse clientAdmin = signup(uniqueEmail(), "Client Org");
        UserSummary clientMember = createUser(clientAdmin.accessToken(), uniqueEmail(), Role.BOARD_MEMBER);

        ResponseEntity<PasswordResetResponse> reset = restTemplate.exchange(
                "/api/users/" + clientMember.id() + "/reset-password", HttpMethod.POST,
                authedRequest(fris.accessToken()), PasswordResetResponse.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);
        String temporaryPassword = reset.getBody().temporaryPassword();
        assertThat(temporaryPassword).isNotBlank();

        ResponseEntity<AuthResponse> loggedIn = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(clientMember.email(), temporaryPassword), AuthResponse.class);
        assertThat(loggedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminOnboardedOrgsSeededUserIsAdminAndCanCreateItsOwnCompanySecretary() {
        AuthResponse frisAdmin = signup(uniqueEmail(), "FRIS Registrar Org");
        String clientAdminEmail = uniqueEmail();

        ResponseEntity<OrganizationOnboardResponse> onboarded = restTemplate.exchange(
                "/api/organizations", HttpMethod.POST,
                authedRequest(frisAdmin.accessToken(),
                        new OrganizationOnboardRequest("Onboarded Client Ltd", "Client", "Admin", clientAdminEmail)),
                OrganizationOnboardResponse.class);
        assertThat(onboarded.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(onboarded.getBody().admin().role()).isEqualTo(Role.ADMIN);
        String temporaryPassword = onboarded.getBody().temporaryPassword();
        assertThat(temporaryPassword).isNotBlank();

        ResponseEntity<AuthResponse> clientAdminLogin = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(clientAdminEmail, temporaryPassword), AuthResponse.class);
        assertThat(clientAdminLogin.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<UserSummary> companySecretary = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(clientAdminLogin.getBody().accessToken(),
                        new CreateUserRequest("New", "Secretary", uniqueEmail(), "password123", Role.COMPANY_SECRETARY)),
                UserSummary.class);
        assertThat(companySecretary.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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
                authedRequest(adminToken, new CreateMeetingRequest(title, null, null, Instant.now(), null, null, defaultMeetingTypeId(adminToken))),
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

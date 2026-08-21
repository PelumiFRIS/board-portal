package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.compliance.ComplianceFilingStatus;
import com.fris.boardportal.compliance.dto.ComplianceFilingSummary;
import com.fris.boardportal.compliance.dto.CreateComplianceFilingRequest;
import com.fris.boardportal.compliance.dto.UpdateComplianceFilingRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ComplianceFilingFlowTest extends IntegrationTestSupport {

    @Test
    void nonAdminCanViewButNotMutate() {
        AuthResponse admin = signup(uniqueEmail(), "Filing View Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<ComplianceFilingSummary[]> list = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.GET, authedRequest(memberAuth.accessToken()),
                ComplianceFilingSummary[].class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> create = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.POST,
                authedRequest(memberAuth.accessToken(),
                        new CreateComplianceFilingRequest("AGM Return", null, LocalDate.now().plusDays(30))),
                String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ComplianceFilingSummary filing = createFiling(admin.accessToken(), "SEC Return", LocalDate.now().plusDays(10));

        ResponseEntity<String> update = restTemplate.exchange(
                "/api/compliance-filings/" + filing.id(), HttpMethod.PATCH,
                authedRequest(memberAuth.accessToken(), new UpdateComplianceFilingRequest("Renamed", null, null)),
                String.class);
        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> submit = restTemplate.exchange(
                "/api/compliance-filings/" + filing.id() + "/submit", HttpMethod.PATCH,
                authedRequest(memberAuth.accessToken()), String.class);
        assertThat(submit.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> delete = restTemplate.exchange(
                "/api/compliance-filings/" + filing.id(), HttpMethod.DELETE,
                authedRequest(memberAuth.accessToken()), String.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanCreateEditSubmitAndDeleteFilings() {
        AuthResponse admin = signup(uniqueEmail(), "Filing Admin Org");

        ComplianceFilingSummary filing = createFiling(admin.accessToken(), "CAC Annual Return", LocalDate.now().plusDays(5));
        assertThat(filing.status()).isEqualTo(ComplianceFilingStatus.PENDING);
        assertThat(filing.submittedAt()).isNull();

        ResponseEntity<ComplianceFilingSummary> updated = restTemplate.exchange(
                "/api/compliance-filings/" + filing.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(),
                        new UpdateComplianceFilingRequest("CAC Annual Return (Revised)", "Updated description", null)),
                ComplianceFilingSummary.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().title()).isEqualTo("CAC Annual Return (Revised)");
        assertThat(updated.getBody().description()).isEqualTo("Updated description");

        ResponseEntity<ComplianceFilingSummary> submitted = restTemplate.exchange(
                "/api/compliance-filings/" + filing.id() + "/submit", HttpMethod.PATCH,
                authedRequest(admin.accessToken()), ComplianceFilingSummary.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submitted.getBody().status()).isEqualTo(ComplianceFilingStatus.SUBMITTED);
        assertThat(submitted.getBody().submittedAt()).isNotNull();
        assertThat(submitted.getBody().submittedByName()).isEqualTo("Ada Admin");

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/compliance-filings/" + filing.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ComplianceFilingSummary[]> list = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.GET, authedRequest(admin.accessToken()),
                ComplianceFilingSummary[].class);
        assertThat(list.getBody()).isEmpty();
    }

    @Test
    void filingsAreScopedToOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Filing Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Filing Org B");

        createFiling(orgAAdmin.accessToken(), "Org A Filing", LocalDate.now().plusDays(1));
        createFiling(orgBAdmin.accessToken(), "Org B Filing", LocalDate.now().plusDays(1));

        ResponseEntity<ComplianceFilingSummary[]> orgAList = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.GET, authedRequest(orgAAdmin.accessToken()),
                ComplianceFilingSummary[].class);
        assertThat(orgAList.getBody()).extracting(ComplianceFilingSummary::title).containsExactly("Org A Filing");
    }

    private ComplianceFilingSummary createFiling(String adminToken, String title, LocalDate dueDate) {
        ResponseEntity<ComplianceFilingSummary> response = restTemplate.exchange(
                "/api/compliance-filings", HttpMethod.POST,
                authedRequest(adminToken, new CreateComplianceFilingRequest(title, null, dueDate)),
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

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}

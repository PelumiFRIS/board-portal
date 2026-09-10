package com.fris.boardportal.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.meeting.dto.MeetingTypeSummary;
import com.fris.boardportal.organization.dto.OrganizationSignupRequest;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Runs against the Postgres started by the project's docker-compose.yml
 * (docker compose up -d postgres) rather than a Testcontainers-managed
 * container. Testcontainers' docker-java client can't complete its Docker
 * handshake against some Docker Desktop builds (confirmed: docker/docker
 * compose CLI work fine against the same daemon), so tests instead reuse
 * the same Postgres already used for local dev, both here and in CI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class IntegrationTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;

    protected AuthResponse signup(String email, String organizationName) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/organizations/signup",
                new OrganizationSignupRequest(organizationName, "Ada", "Admin", email, "password123"),
                AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    protected UUID defaultMeetingTypeId(String token) {
        ResponseEntity<MeetingTypeSummary[]> response = restTemplate.exchange(
                "/api/meeting-types", HttpMethod.GET, authedRequest(token), MeetingTypeSummary[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MeetingTypeSummary[] types = response.getBody();
        assertThat(types).isNotEmpty();
        return types[0].id();
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    /**
     * Governance-content mutations (meetings, resolutions, documents, resources, committees,
     * compliance filings, conflict declarations, meeting types, recordings, action items) are
     * Company Secretary-only; Admin keeps account/tenant administration only. Use this wherever
     * a test needs an actor who can create/edit/delete that content.
     */
    protected AuthResponse createCompanySecretaryAndLogin(String adminToken) {
        String email = uniqueEmail();
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Secretary", email, "password123", Role.COMPANY_SECRETARY)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginResponse.getBody();
    }

    protected <T> HttpEntity<T> authedRequest(String token) {
        return authedRequest(token, null);
    }

    protected <T> HttpEntity<T> authedRequest(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}

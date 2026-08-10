package com.fris.boardportal.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.organization.dto.OrganizationSignupRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
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

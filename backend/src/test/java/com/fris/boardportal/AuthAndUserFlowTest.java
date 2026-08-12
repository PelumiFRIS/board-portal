package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.organization.dto.OrganizationSignupRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.UserStatus;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UpdateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthAndUserFlowTest extends IntegrationTestSupport {

    @Test
    void signupCreatesOrganizationAndAdminUser() {
        AuthResponse response = signup(uniqueEmail(), "Acme Governance");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().role()).isEqualTo(Role.ADMIN);
        assertThat(response.user().organizationName()).isEqualTo("Acme Governance");
    }

    @Test
    void signupWithDuplicateEmailIsRejected() {
        String email = uniqueEmail();
        signup(email, "First Org");

        ResponseEntity<String> second = restTemplate.postForEntity(
                "/api/organizations/signup",
                new OrganizationSignupRequest("Second Org", "Jane", "Doe", email, "password123"),
                String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginFailsWithWrongPassword() {
        String email = uniqueEmail();
        signup(email, "Login Test Org");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "wrong-password"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        String email = uniqueEmail();
        signup(email, "Login OK Org");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isNotBlank();
    }

    @Test
    void meRequiresAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meWithInvalidTokenReturnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, authedRequest("not-a-real-token"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meReturnsCurrentUser() {
        String email = uniqueEmail();
        AuthResponse auth = signup(email, "Me Org");

        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, authedRequest(auth.accessToken()), UserSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualToIgnoringCase(email);
    }

    @Test
    void adminCanCreateAndListUsersInOwnOrganization() {
        AuthResponse admin = signup(uniqueEmail(), "Roster Org");

        ResponseEntity<UserSummary> created = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateUserRequest("Board", "Member", uniqueEmail(), "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<UserSummary[]> list = restTemplate.exchange(
                "/api/users", HttpMethod.GET, authedRequest(admin.accessToken()), UserSummary[].class);
        assertThat(list.getBody()).hasSize(2);
    }

    @Test
    void nonAdminCannotListUsers() {
        AuthResponse admin = signup(uniqueEmail(), "Restricted Org");
        String memberEmail = uniqueEmail();
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateUserRequest("Board", "Member", memberEmail, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);

        ResponseEntity<AuthResponse> memberLogin = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(memberEmail, "password123"), AuthResponse.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users", HttpMethod.GET,
                authedRequest(memberLogin.getBody().accessToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCannotModifyUserFromAnotherOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Org B");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + orgBAdmin.user().id(), HttpMethod.PATCH,
                authedRequest(orgAAdmin.accessToken(), new UpdateUserRequest(null, UserStatus.DISABLED)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}

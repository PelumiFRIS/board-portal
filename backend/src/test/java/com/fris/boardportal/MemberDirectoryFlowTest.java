package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
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

class MemberDirectoryFlowTest extends IntegrationTestSupport {

    @Test
    void nonAdminCanViewDirectoryButNotTheRoster() {
        AuthResponse admin = signup(uniqueEmail(), "Directory Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        ResponseEntity<UserSummary[]> directory = restTemplate.exchange(
                "/api/users/directory", HttpMethod.GET, authedRequest(member.accessToken()), UserSummary[].class);
        assertThat(directory.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(directory.getBody()).hasSize(2);

        ResponseEntity<String> roster = restTemplate.exchange(
                "/api/users", HttpMethod.GET, authedRequest(member.accessToken()), String.class);
        assertThat(roster.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void directoryExcludesDeactivatedMembers() {
        AuthResponse admin = signup(uniqueEmail(), "Deactivation Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);

        restTemplate.exchange(
                "/api/users/" + member.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(), new UpdateUserRequest(null, UserStatus.DISABLED, null, null, null, null)),
                UserSummary.class);

        ResponseEntity<UserSummary[]> directory = restTemplate.exchange(
                "/api/users/directory", HttpMethod.GET, authedRequest(admin.accessToken()), UserSummary[].class);
        assertThat(directory.getBody()).extracting(UserSummary::id).doesNotContain(member.id());
    }

    @Test
    void adminCanEditProfileFieldsAndNonAdminCannot() {
        AuthResponse admin = signup(uniqueEmail(), "Profile Edit Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/users/" + member.id(), HttpMethod.PATCH,
                authedRequest(memberAuth.accessToken(),
                        new UpdateUserRequest(null, null, "Independent Director", null, null, null)),
                String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<UserSummary> updated = restTemplate.exchange(
                "/api/users/" + member.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(),
                        new UpdateUserRequest(null, null, "Independent Director", "+234-800-000-0000",
                                "Governance professional.", "Audit Committee")),
                UserSummary.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().title()).isEqualTo("Independent Director");
        assertThat(updated.getBody().phone()).isEqualTo("+234-800-000-0000");
        assertThat(updated.getBody().bio()).isEqualTo("Governance professional.");
        assertThat(updated.getBody().committees()).isEqualTo("Audit Committee");

        ResponseEntity<UserSummary[]> directory = restTemplate.exchange(
                "/api/users/directory", HttpMethod.GET, authedRequest(admin.accessToken()), UserSummary[].class);
        assertThat(directory.getBody())
                .filteredOn(u -> u.id().equals(member.id()))
                .extracting(UserSummary::title)
                .containsExactly("Independent Director");
    }

    @Test
    void adminCannotSeeAnotherOrganizationsDirectory() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Directory Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Directory Org B");
        createBoardMember(orgBAdmin.accessToken(), uniqueEmail());

        ResponseEntity<UserSummary[]> orgADirectory = restTemplate.exchange(
                "/api/users/directory", HttpMethod.GET, authedRequest(orgAAdmin.accessToken()), UserSummary[].class);
        assertThat(orgADirectory.getBody()).hasSize(1);
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

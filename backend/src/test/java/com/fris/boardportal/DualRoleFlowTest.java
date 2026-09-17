package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.committee.dto.CreateCommitteeRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UpdateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A user's role (BOARD_MEMBER/COMPANY_SECRETARY/ADMIN) and the additive isAdmin
 * flag are independent: isAdmin grants Admin rights on top of whatever the
 * content role already allows, without replacing it. See AppUserPrincipal.
 */
class DualRoleFlowTest extends IntegrationTestSupport {

    @Test
    void plainCompanySecretaryCanManageContentButNotAccounts() {
        AuthResponse admin = signup(uniqueEmail(), "Dual Role Org A");
        String csEmail = uniqueEmail();
        createUser(admin.accessToken(), csEmail, Role.COMPANY_SECRETARY, null);
        AuthResponse cs = login(csEmail);

        assertThat(createCommittee(cs.accessToken()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(listUsers(cs.accessToken()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void companySecretaryWithIsAdminCanDoBoth() {
        AuthResponse admin = signup(uniqueEmail(), "Dual Role Org B");
        String email = uniqueEmail();
        createUser(admin.accessToken(), email, Role.COMPANY_SECRETARY, true);
        AuthResponse dualRole = login(email);

        assertThat(createCommittee(dualRole.accessToken()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(listUsers(dualRole.accessToken()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void onlyAnAdminCanGrantTheIsAdminFlag() {
        AuthResponse admin = signup(uniqueEmail(), "Dual Role Org C");
        String email = uniqueEmail();
        ResponseEntity<UserSummary> created = createUser(admin.accessToken(), email, Role.COMPANY_SECRETARY, null);
        UserSummary target = created.getBody();
        AuthResponse plainCs = login(email);

        ResponseEntity<String> selfPromote = restTemplate.exchange(
                "/api/users/" + target.id(), HttpMethod.PATCH,
                authedRequest(plainCs.accessToken(), new UpdateUserRequest(null, null, null, null, null, true)),
                String.class);
        assertThat(selfPromote.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<UserSummary> adminGrant = restTemplate.exchange(
                "/api/users/" + target.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(), new UpdateUserRequest(null, null, null, null, null, true)),
                UserSummary.class);
        assertThat(adminGrant.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminGrant.getBody().isAdmin()).isTrue();
    }

    private ResponseEntity<UserSummary> createUser(String adminToken, String email, Role role, Boolean isAdmin) {
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Dual", "Role", email, "password123", role, isAdmin)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response;
    }

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<String> createCommittee(String token) {
        return restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(token, new CreateCommitteeRequest("Audit Committee", null, null)),
                String.class);
    }

    private ResponseEntity<String> listUsers(String token) {
        return restTemplate.exchange("/api/users", HttpMethod.GET, authedRequest(token), String.class);
    }
}

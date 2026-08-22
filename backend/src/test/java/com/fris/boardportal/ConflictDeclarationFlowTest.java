package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.conflict.dto.ConflictDeclarationSummary;
import com.fris.boardportal.conflict.dto.CreateConflictDeclarationRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ConflictDeclarationFlowTest extends IntegrationTestSupport {

    @Test
    void memberCanDeclareForThemselvesAndSeeOwnHistory() {
        AuthResponse admin = signup(uniqueEmail(), "Conflict Self Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<ConflictDeclarationSummary> declared = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.POST,
                authedRequest(memberAuth.accessToken(), new CreateConflictDeclarationRequest(null, false, null)),
                ConflictDeclarationSummary.class);
        assertThat(declared.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(declared.getBody().hasConflict()).isFalse();

        ResponseEntity<ConflictDeclarationSummary[]> mine = restTemplate.exchange(
                "/api/conflict-declarations/me", HttpMethod.GET, authedRequest(memberAuth.accessToken()),
                ConflictDeclarationSummary[].class);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody()).hasSize(1);
        assertThat(mine.getBody()[0].declaredByName()).isEqualTo("Board Member");
    }

    @Test
    void memberCannotDeclareForSomeoneElseOrViewFullOrgList() {
        AuthResponse admin = signup(uniqueEmail(), "Conflict Restrict Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        String otherEmail = uniqueEmail();
        UserSummary other = createBoardMember(admin.accessToken(), otherEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<String> blockedDeclare = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.POST,
                authedRequest(memberAuth.accessToken(), new CreateConflictDeclarationRequest(other.id(), false, null)),
                String.class);
        assertThat(blockedDeclare.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> blockedList = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.GET, authedRequest(memberAuth.accessToken()), String.class);
        assertThat(blockedList.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(member.id()).isNotNull();
    }

    @Test
    void adminCanDeclareOnBehalfAndReviewFullOrgList() {
        AuthResponse admin = signup(uniqueEmail(), "Conflict Admin Org");
        UserSummary member = createBoardMember(admin.accessToken(), uniqueEmail());

        ResponseEntity<ConflictDeclarationSummary> declared = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateConflictDeclarationRequest(member.id(), true, "Spouse works at a vendor")),
                ConflictDeclarationSummary.class);
        assertThat(declared.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(declared.getBody().userId()).isEqualTo(member.id());
        assertThat(declared.getBody().hasConflict()).isTrue();
        assertThat(declared.getBody().details()).isEqualTo("Spouse works at a vendor");
        assertThat(declared.getBody().declaredByName()).isEqualTo("Ada Admin");

        ResponseEntity<ConflictDeclarationSummary[]> all = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.GET, authedRequest(admin.accessToken()),
                ConflictDeclarationSummary[].class);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(all.getBody()).hasSize(1);
        assertThat(all.getBody()[0].userName()).isEqualTo("Board Member");
    }

    @Test
    void declarationsAreScopedToOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Conflict Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Conflict Org B");

        restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.POST,
                authedRequest(orgAAdmin.accessToken(), new CreateConflictDeclarationRequest(null, false, null)),
                ConflictDeclarationSummary.class);
        restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.POST,
                authedRequest(orgBAdmin.accessToken(), new CreateConflictDeclarationRequest(null, false, null)),
                ConflictDeclarationSummary.class);

        ResponseEntity<ConflictDeclarationSummary[]> orgAList = restTemplate.exchange(
                "/api/conflict-declarations", HttpMethod.GET, authedRequest(orgAAdmin.accessToken()),
                ConflictDeclarationSummary[].class);
        assertThat(orgAList.getBody()).hasSize(1);
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

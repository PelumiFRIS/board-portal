package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.committee.dto.AddCommitteeMemberRequest;
import com.fris.boardportal.committee.dto.CommitteeSummary;
import com.fris.boardportal.committee.dto.CreateCommitteeRequest;
import com.fris.boardportal.committee.dto.UpdateCommitteeRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CommitteeFlowTest extends IntegrationTestSupport {

    @Test
    void nonAdminCanViewButNotMutate() {
        AuthResponse admin = signup(uniqueEmail(), "Committee View Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<CommitteeSummary[]> list = restTemplate.exchange(
                "/api/committees", HttpMethod.GET, authedRequest(memberAuth.accessToken()), CommitteeSummary[].class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> create = restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(memberAuth.accessToken(), new CreateCommitteeRequest("Audit", null)), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        CommitteeSummary committee = createCommittee(admin.accessToken(), "Risk", null);

        ResponseEntity<String> update = restTemplate.exchange(
                "/api/committees/" + committee.id(), HttpMethod.PATCH,
                authedRequest(memberAuth.accessToken(), new UpdateCommitteeRequest("Renamed", null)), String.class);
        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> addMember = restTemplate.exchange(
                "/api/committees/" + committee.id() + "/members", HttpMethod.POST,
                authedRequest(memberAuth.accessToken(), new AddCommitteeMemberRequest(member.id())), String.class);
        assertThat(addMember.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> removeMember = restTemplate.exchange(
                "/api/committees/" + committee.id() + "/members/" + member.id(), HttpMethod.DELETE,
                authedRequest(memberAuth.accessToken()), String.class);
        assertThat(removeMember.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> setChair = restTemplate.exchange(
                "/api/committees/" + committee.id() + "/members/" + member.id() + "/chair", HttpMethod.PATCH,
                authedRequest(memberAuth.accessToken()), String.class);
        assertThat(setChair.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> delete = restTemplate.exchange(
                "/api/committees/" + committee.id(), HttpMethod.DELETE,
                authedRequest(memberAuth.accessToken()), String.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanManageMembershipAndOnlyOneChairAtATime() {
        AuthResponse admin = signup(uniqueEmail(), "Committee Chair Org");
        UserSummary memberA = createBoardMember(admin.accessToken(), uniqueEmail());
        UserSummary memberB = createBoardMember(admin.accessToken(), uniqueEmail());

        CommitteeSummary committee = createCommittee(admin.accessToken(), "Audit Committee", "Oversees audits");
        assertThat(committee.members()).isEmpty();

        committee = addMember(admin.accessToken(), committee.id(), memberA.id());
        committee = addMember(admin.accessToken(), committee.id(), memberB.id());
        assertThat(committee.members()).hasSize(2);
        assertThat(committee.members()).noneMatch(m -> m.isChair());

        committee = setChair(admin.accessToken(), committee.id(), memberA.id());
        assertThat(committee.members())
                .filteredOn(m -> m.userId().equals(memberA.id()))
                .allMatch(m -> m.isChair());
        assertThat(committee.members())
                .filteredOn(m -> m.userId().equals(memberB.id()))
                .allMatch(m -> !m.isChair());

        committee = setChair(admin.accessToken(), committee.id(), memberB.id());
        assertThat(committee.members())
                .filteredOn(m -> m.userId().equals(memberB.id()))
                .allMatch(m -> m.isChair());
        assertThat(committee.members())
                .filteredOn(m -> m.userId().equals(memberA.id()))
                .allMatch(m -> !m.isChair());

        ResponseEntity<CommitteeSummary> afterRemove = restTemplate.exchange(
                "/api/committees/" + committee.id() + "/members/" + memberA.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), CommitteeSummary.class);
        assertThat(afterRemove.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterRemove.getBody().members()).extracting(m -> m.userId()).containsExactly(memberB.id());

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/committees/" + committee.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<CommitteeSummary[]> list = restTemplate.exchange(
                "/api/committees", HttpMethod.GET, authedRequest(admin.accessToken()), CommitteeSummary[].class);
        assertThat(list.getBody()).isEmpty();
    }

    @Test
    void userSummaryReflectsCurrentMemberships() {
        AuthResponse admin = signup(uniqueEmail(), "Committee Summary Org");
        UserSummary member = createBoardMember(admin.accessToken(), uniqueEmail());

        CommitteeSummary committee = createCommittee(admin.accessToken(), "Risk Committee", null);
        addMember(admin.accessToken(), committee.id(), member.id());
        setChair(admin.accessToken(), committee.id(), member.id());

        ResponseEntity<UserSummary[]> directory = restTemplate.exchange(
                "/api/users/directory", HttpMethod.GET, authedRequest(admin.accessToken()), UserSummary[].class);
        UserSummary memberSummary = Arrays.stream(directory.getBody())
                .filter(u -> u.id().equals(member.id()))
                .findFirst()
                .orElseThrow();
        assertThat(memberSummary.committees()).hasSize(1);
        assertThat(memberSummary.committees().get(0).committeeName()).isEqualTo("Risk Committee");
        assertThat(memberSummary.committees().get(0).isChair()).isTrue();
    }

    @Test
    void committeesAreScopedToOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Committee Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Committee Org B");

        createCommittee(orgAAdmin.accessToken(), "Org A Committee", null);
        createCommittee(orgBAdmin.accessToken(), "Org B Committee", null);

        ResponseEntity<CommitteeSummary[]> orgAList = restTemplate.exchange(
                "/api/committees", HttpMethod.GET, authedRequest(orgAAdmin.accessToken()), CommitteeSummary[].class);
        assertThat(orgAList.getBody()).extracting(CommitteeSummary::name).containsExactly("Org A Committee");
    }

    private CommitteeSummary createCommittee(String adminToken, String name, String description) {
        ResponseEntity<CommitteeSummary> response = restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(adminToken, new CreateCommitteeRequest(name, description)), CommitteeSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private CommitteeSummary addMember(String adminToken, UUID committeeId, UUID userId) {
        ResponseEntity<CommitteeSummary> response = restTemplate.exchange(
                "/api/committees/" + committeeId + "/members", HttpMethod.POST,
                authedRequest(adminToken, new AddCommitteeMemberRequest(userId)), CommitteeSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private CommitteeSummary setChair(String adminToken, UUID committeeId, UUID userId) {
        ResponseEntity<CommitteeSummary> response = restTemplate.exchange(
                "/api/committees/" + committeeId + "/members/" + userId + "/chair", HttpMethod.PATCH,
                authedRequest(adminToken), CommitteeSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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

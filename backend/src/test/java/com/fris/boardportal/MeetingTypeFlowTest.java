package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingTypeRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.meeting.dto.MeetingTypeSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class MeetingTypeFlowTest extends IntegrationTestSupport {

    @Test
    void newOrganizationGetsFourDefaultMeetingTypes() {
        AuthResponse admin = signup(uniqueEmail(), "Default Types Org");

        ResponseEntity<MeetingTypeSummary[]> response = restTemplate.exchange(
                "/api/meeting-types", HttpMethod.GET, authedRequest(admin.accessToken()), MeetingTypeSummary[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(MeetingTypeSummary::name).containsExactlyInAnyOrder(
                "Board Meeting", "Committee Meeting", "Executive/Management Meeting", "General Staff Meeting");
    }

    @Test
    void adminCanAddAndDeleteMeetingTypes() {
        AuthResponse admin = signup(uniqueEmail(), "Meeting Type Manage Org");

        ResponseEntity<MeetingTypeSummary> created = restTemplate.exchange(
                "/api/meeting-types", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateMeetingTypeRequest("Strategy Retreat")),
                MeetingTypeSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().name()).isEqualTo("Strategy Retreat");

        ResponseEntity<MeetingTypeSummary[]> afterCreate = restTemplate.exchange(
                "/api/meeting-types", HttpMethod.GET, authedRequest(admin.accessToken()), MeetingTypeSummary[].class);
        assertThat(afterCreate.getBody()).extracting(MeetingTypeSummary::name).contains("Strategy Retreat");

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/meeting-types/" + created.getBody().id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<MeetingTypeSummary[]> afterDelete = restTemplate.exchange(
                "/api/meeting-types", HttpMethod.GET, authedRequest(admin.accessToken()), MeetingTypeSummary[].class);
        assertThat(afterDelete.getBody()).extracting(MeetingTypeSummary::name).doesNotContain("Strategy Retreat");
    }

    @Test
    void nonAdminCanViewButNotMutateMeetingTypes() {
        AuthResponse admin = signup(uniqueEmail(), "Meeting Type Restricted Org");
        String memberEmail = uniqueEmail();
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateUserRequest("Board", "Member", memberEmail, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(memberEmail, "password123"), AuthResponse.class);
        String memberToken = loginResponse.getBody().accessToken();

        ResponseEntity<MeetingTypeSummary[]> list = restTemplate.exchange(
                "/api/meeting-types", HttpMethod.GET, authedRequest(memberToken), MeetingTypeSummary[].class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> create = restTemplate.exchange(
                "/api/meeting-types", HttpMethod.POST,
                authedRequest(memberToken, new CreateMeetingTypeRequest("Unauthorized Type")), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deletingAMeetingTypeInUseIsRejected() {
        AuthResponse admin = signup(uniqueEmail(), "Meeting Type In Use Org");
        UUID typeId = defaultMeetingTypeId(admin.accessToken());

        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> meeting = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateMeetingRequest("Q4 Board Meeting", null, null, start, null, null, typeId)),
                MeetingSummary.class);
        assertThat(meeting.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> deleteAttempt = restTemplate.exchange(
                "/api/meeting-types/" + typeId, HttpMethod.DELETE, authedRequest(admin.accessToken()), String.class);
        assertThat(deleteAttempt.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void creatingADuplicateMeetingTypeNameIsRejected() {
        AuthResponse admin = signup(uniqueEmail(), "Meeting Type Duplicate Org");

        ResponseEntity<String> duplicate = restTemplate.exchange(
                "/api/meeting-types", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateMeetingTypeRequest("Board Meeting")), String.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

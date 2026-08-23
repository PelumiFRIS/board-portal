package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.meeting.MeetingStatus;
import com.fris.boardportal.meeting.dto.AgendaItemDto;
import com.fris.boardportal.meeting.dto.CreateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingDetail;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.meeting.dto.UpdateAgendaItemRequest;
import com.fris.boardportal.meeting.dto.UpdateMeetingRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class MeetingFlowTest extends IntegrationTestSupport {

    @Test
    void adminCanScheduleAndAnyOrgMemberCanView() {
        AuthResponse admin = signup(uniqueEmail(), "Board Co");
        MeetingSummary created = scheduleMeeting(admin.accessToken());

        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        ResponseEntity<MeetingDetail> response = restTemplate.exchange(
                "/api/meetings/" + created.id(), HttpMethod.GET, authedRequest(member.accessToken()),
                MeetingDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().title()).isEqualTo("Q3 Board Meeting");
        assertThat(response.getBody().status()).isEqualTo(MeetingStatus.SCHEDULED);
    }

    @Test
    void nonAdminCannotScheduleMeeting() {
        AuthResponse admin = signup(uniqueEmail(), "Restricted Meetings Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(member.accessToken(), newMeetingRequest()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminManagesAgendaItemsAndMinutes() {
        AuthResponse admin = signup(uniqueEmail(), "Agenda Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());

        ResponseEntity<AgendaItemDto> item = restTemplate.exchange(
                "/api/meetings/" + meeting.id() + "/agenda-items", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateAgendaItemRequest("Approve budget", null, null)),
                AgendaItemDto.class);
        assertThat(item.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AgendaItemDto> updatedItem = restTemplate.exchange(
                "/api/meetings/" + meeting.id() + "/agenda-items/" + item.getBody().id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(), new UpdateAgendaItemRequest("Approve FY26 budget", null, null)),
                AgendaItemDto.class);
        assertThat(updatedItem.getBody().title()).isEqualTo("Approve FY26 budget");

        ResponseEntity<MeetingDetail> completed = restTemplate.exchange(
                "/api/meetings/" + meeting.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(),
                        new UpdateMeetingRequest(null, null, null, null, null, MeetingStatus.COMPLETED, "Budget approved unanimously.")),
                MeetingDetail.class);
        assertThat(completed.getBody().status()).isEqualTo(MeetingStatus.COMPLETED);
        assertThat(completed.getBody().minutesContent()).isEqualTo("Budget approved unanimously.");
        assertThat(completed.getBody().agendaItems()).hasSize(1);

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/meetings/" + meeting.id() + "/agenda-items/" + item.getBody().id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void adminCannotAccessMeetingFromAnotherOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Meetings Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Meetings Org B");
        MeetingSummary orgBMeeting = scheduleMeeting(orgBAdmin.accessToken());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/meetings/" + orgBMeeting.id(), HttpMethod.GET,
                authedRequest(orgAAdmin.accessToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anyOrgMemberCanDownloadIcsButNotForAnotherOrgsMeeting() {
        AuthResponse admin = signup(uniqueEmail(), "Calendar Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());

        ResponseEntity<String> ics = restTemplate.exchange(
                "/api/meetings/" + meeting.id() + "/ics", HttpMethod.GET,
                authedRequest(member.accessToken()), String.class);
        assertThat(ics.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ics.getHeaders().getContentType()).isNotNull();
        assertThat(ics.getHeaders().getContentType().toString()).contains("text/calendar");
        assertThat(ics.getBody()).contains("BEGIN:VCALENDAR");
        assertThat(ics.getBody()).contains("SUMMARY:Q3 Board Meeting");
        assertThat(ics.getBody()).contains("UID:" + meeting.id() + "@board-portal");

        AuthResponse otherOrgAdmin = signup(uniqueEmail(), "Calendar Org B");
        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/meetings/" + meeting.id() + "/ics", HttpMethod.GET,
                authedRequest(otherOrgAdmin.accessToken()), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private MeetingSummary scheduleMeeting(String adminToken) {
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST, authedRequest(adminToken, newMeetingRequest()), MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private CreateMeetingRequest newMeetingRequest() {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        return new CreateMeetingRequest("Q3 Board Meeting", "Quarterly review", "Virtual", start,
                start.plus(1, ChronoUnit.HOURS));
    }

    private void createBoardMember(String adminToken, String email) {
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
    }

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}

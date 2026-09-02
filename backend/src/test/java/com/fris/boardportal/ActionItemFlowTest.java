package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.actionitem.ActionItemStatus;
import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.actionitem.dto.CreateActionItemRequest;
import com.fris.boardportal.actionitem.dto.UpdateActionItemStatusRequest;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingDetail;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ActionItemFlowTest extends IntegrationTestSupport {

    @Test
    void nonAdminCannotCreateOrDeleteActionItems() {
        AuthResponse admin = signup(uniqueEmail(), "Restricted Action Items Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        String memberEmail = uniqueEmail();
        UUID memberId = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/action-items", HttpMethod.POST,
                authedRequest(member.accessToken(),
                        new CreateActionItemRequest(meeting.id(), "Follow up", null, memberId, null)),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ActionItemSummary item = createActionItem(admin.accessToken(), meeting.id(), memberId);

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/action-items/" + item.id(), HttpMethod.DELETE,
                authedRequest(member.accessToken()), String.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void assigneeCanToggleStatusButOtherMembersCannot() {
        AuthResponse admin = signup(uniqueEmail(), "Toggle Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());

        String assigneeEmail = uniqueEmail();
        UUID assigneeId = createBoardMember(admin.accessToken(), assigneeEmail);
        AuthResponse assignee = login(assigneeEmail);

        String otherEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), otherEmail);
        AuthResponse other = login(otherEmail);

        ActionItemSummary item = createActionItem(admin.accessToken(), meeting.id(), assigneeId);
        assertThat(item.status()).isEqualTo(ActionItemStatus.OPEN);

        // a different, non-assignee member cannot toggle it
        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/action-items/" + item.id() + "/status", HttpMethod.PATCH,
                authedRequest(other.accessToken(), new UpdateActionItemStatusRequest(ActionItemStatus.DONE)),
                String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // the assignee can mark it done
        ResponseEntity<ActionItemSummary> markedDone = restTemplate.exchange(
                "/api/action-items/" + item.id() + "/status", HttpMethod.PATCH,
                authedRequest(assignee.accessToken(), new UpdateActionItemStatusRequest(ActionItemStatus.DONE)),
                ActionItemSummary.class);
        assertThat(markedDone.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(markedDone.getBody().status()).isEqualTo(ActionItemStatus.DONE);

        // admin can reopen it
        ResponseEntity<ActionItemSummary> reopened = restTemplate.exchange(
                "/api/action-items/" + item.id() + "/status", HttpMethod.PATCH,
                authedRequest(admin.accessToken(), new UpdateActionItemStatusRequest(ActionItemStatus.OPEN)),
                ActionItemSummary.class);
        assertThat(reopened.getBody().status()).isEqualTo(ActionItemStatus.OPEN);
    }

    @Test
    void actionItemAppearsOnItsMeetingAndCanBeDeletedByAdmin() {
        AuthResponse admin = signup(uniqueEmail(), "Meeting Integration Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        String memberEmail = uniqueEmail();
        UUID memberId = createBoardMember(admin.accessToken(), memberEmail);

        ActionItemSummary item = createActionItem(admin.accessToken(), meeting.id(), memberId);

        ResponseEntity<MeetingDetail> detail = restTemplate.exchange(
                "/api/meetings/" + meeting.id(), HttpMethod.GET,
                authedRequest(admin.accessToken()), MeetingDetail.class);
        assertThat(detail.getBody().actionItems()).extracting(ActionItemSummary::id).contains(item.id());
        assertThat(detail.getBody().actionItems().get(0).assigneeName()).isEqualTo("Board Member");

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/action-items/" + item.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void adminCanExportActionItemsCsvButNonAdminCannot() {
        AuthResponse admin = signup(uniqueEmail(), "Export Action Items Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        String memberEmail = uniqueEmail();
        UUID memberId = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);
        createActionItem(admin.accessToken(), meeting.id(), memberId);

        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/action-items/export", HttpMethod.GET, authedRequest(member.accessToken()), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> exported = restTemplate.exchange(
                "/api/action-items/export", HttpMethod.GET, authedRequest(admin.accessToken()), String.class);
        assertThat(exported.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exported.getHeaders().getContentType()).isNotNull();
        assertThat(exported.getHeaders().getContentType().toString()).contains("text/csv");
        assertThat(exported.getBody()).contains("Meeting,Title,Assignee,Due Date,Status");
        assertThat(exported.getBody()).contains("Action Item Test Meeting");
        assertThat(exported.getBody()).contains("Finalize the draft");
        assertThat(exported.getBody()).contains("Board Member");
    }

    @Test
    void adminCannotAccessActionItemFromAnotherOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Action Items Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Action Items Org B");
        MeetingSummary orgBMeeting = scheduleMeeting(orgBAdmin.accessToken());
        UUID orgBMemberId = createBoardMember(orgBAdmin.accessToken(), uniqueEmail());
        ActionItemSummary orgBItem = createActionItem(orgBAdmin.accessToken(), orgBMeeting.id(), orgBMemberId);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/action-items/" + orgBItem.id() + "/status", HttpMethod.PATCH,
                authedRequest(orgAAdmin.accessToken(), new UpdateActionItemStatusRequest(ActionItemStatus.DONE)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private MeetingSummary scheduleMeeting(String adminToken) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest("Action Item Test Meeting", null, null, start, null, null, defaultMeetingTypeId(adminToken))),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ActionItemSummary createActionItem(String adminToken, UUID meetingId, UUID assigneeId) {
        ResponseEntity<ActionItemSummary> response = restTemplate.exchange(
                "/api/action-items", HttpMethod.POST,
                authedRequest(adminToken, new CreateActionItemRequest(meetingId, "Finalize the draft", null,
                        assigneeId, LocalDate.now().plusDays(14))),
                ActionItemSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private UUID createBoardMember(String adminToken, String email) {
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}

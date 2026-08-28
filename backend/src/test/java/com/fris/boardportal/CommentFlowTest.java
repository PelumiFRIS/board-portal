package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.comment.CommentEntityType;
import com.fris.boardportal.comment.dto.CommentDto;
import com.fris.boardportal.comment.dto.CreateCommentRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
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
import org.springframework.web.util.UriComponentsBuilder;

class CommentFlowTest extends IntegrationTestSupport {

    @Test
    void anyOrgMemberCanPostAndThreadIsOldestFirst() {
        AuthResponse admin = signup(uniqueEmail(), "Comment Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        CommentDto first = postComment(admin.accessToken(), meeting.id(), "Let's discuss the budget line items.");
        CommentDto second = postComment(member.accessToken(), meeting.id(), "Happy to walk through my section.");

        ResponseEntity<CommentDto[]> thread = restTemplate.exchange(
                threadUrl(meeting.id()), HttpMethod.GET, authedRequest(member.accessToken()), CommentDto[].class);
        assertThat(thread.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(thread.getBody()).extracting(CommentDto::id).containsExactly(first.id(), second.id());
        assertThat(thread.getBody()[1].authorName()).isEqualTo("Board Member");
    }

    @Test
    void authorCanDeleteOwnCommentButNotSomeoneElses() {
        AuthResponse admin = signup(uniqueEmail(), "Delete Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse member = login(memberEmail);

        CommentDto adminComment = postComment(admin.accessToken(), meeting.id(), "Admin's note.");
        CommentDto memberComment = postComment(member.accessToken(), meeting.id(), "Member's note.");

        // member cannot delete admin's comment
        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/comments/" + adminComment.id(), HttpMethod.DELETE,
                authedRequest(member.accessToken()), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // member can delete their own comment
        ResponseEntity<Void> ownDelete = restTemplate.exchange(
                "/api/comments/" + memberComment.id(), HttpMethod.DELETE,
                authedRequest(member.accessToken()), Void.class);
        assertThat(ownDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // admin can delete anyone's comment (moderation)
        ResponseEntity<Void> adminDelete = restTemplate.exchange(
                "/api/comments/" + adminComment.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(adminDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void commentingOnAnotherOrganizationsMeetingIsRejected() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Comment Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Comment Org B");
        MeetingSummary orgBMeeting = scheduleMeeting(orgBAdmin.accessToken());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/comments", HttpMethod.POST,
                authedRequest(orgAAdmin.accessToken(),
                        new CreateCommentRequest(CommentEntityType.MEETING, orgBMeeting.id(), "Sneaky comment")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private MeetingSummary scheduleMeeting(String adminToken) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest("Comment Test Meeting", null, null, start, null, null, null)),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private CommentDto postComment(String token, java.util.UUID meetingId, String body) {
        ResponseEntity<CommentDto> response = restTemplate.exchange(
                "/api/comments", HttpMethod.POST,
                authedRequest(token, new CreateCommentRequest(CommentEntityType.MEETING, meetingId, body)),
                CommentDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private String threadUrl(java.util.UUID meetingId) {
        return UriComponentsBuilder.fromPath("/api/comments")
                .queryParam("entityType", "MEETING")
                .queryParam("entityId", meetingId)
                .toUriString();
    }

    private void createBoardMember(String adminToken, String email) {
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}

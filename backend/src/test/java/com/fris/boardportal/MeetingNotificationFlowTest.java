package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.committee.dto.AddCommitteeMemberRequest;
import com.fris.boardportal.committee.dto.CommitteeSummary;
import com.fris.boardportal.committee.dto.CreateCommitteeRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MeetingNotificationFlowTest extends IntegrationTestSupport {

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void schedulingAMeetingEmailsActiveMembersInBcc() {
        AuthResponse admin = signup(uniqueEmail(), "Notify Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);

        MeetingSummary meeting = scheduleMeeting(admin.accessToken());

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sent = messageCaptor.getValue();
        assertThat(sent.getSubject()).contains(meeting.title());
        assertThat(sent.getBcc()).contains(admin.user().email(), memberEmail);
    }

    @Test
    void committeeScopedMeetingEmailsOnlyCommitteeMembers() {
        AuthResponse admin = signup(uniqueEmail(), "Committee Notify Org");
        String memberAEmail = uniqueEmail();
        UserSummary memberA = createBoardMemberSummary(admin.accessToken(), memberAEmail);
        String memberBEmail = uniqueEmail();
        createBoardMemberSummary(admin.accessToken(), memberBEmail);

        CommitteeSummary committee = createCommittee(admin.accessToken(), "Audit Committee");
        addCommitteeMember(admin.accessToken(), committee.id(), memberA.id());

        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateMeetingRequest("Audit Committee Meeting", null, null,
                        start, start.plus(1, ChronoUnit.HOURS), committee.id())),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sent = messageCaptor.getValue();
        assertThat(sent.getBcc()).containsExactly(memberAEmail);
        assertThat(sent.getBcc()).doesNotContain(memberBEmail, admin.user().email());
    }

    @Test
    void meetingCreationSucceedsEvenIfEmailSendingFails() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        AuthResponse admin = signup(uniqueEmail(), "Notify Failure Org");
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST, authedRequest(admin.accessToken(), newMeetingRequest()),
                MeetingSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private MeetingSummary scheduleMeeting(String adminToken) {
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST, authedRequest(adminToken, newMeetingRequest()), MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private CreateMeetingRequest newMeetingRequest() {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        return new CreateMeetingRequest("Notify Test Meeting", "Quarterly review", "Virtual", start,
                start.plus(1, ChronoUnit.HOURS), null);
    }

    private void createBoardMember(String adminToken, String email) {
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
    }

    private UserSummary createBoardMemberSummary(String adminToken, String email) {
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private CommitteeSummary createCommittee(String adminToken, String name) {
        ResponseEntity<CommitteeSummary> response = restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(adminToken, new CreateCommitteeRequest(name, null, null)), CommitteeSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void addCommitteeMember(String adminToken, java.util.UUID committeeId, java.util.UUID userId) {
        ResponseEntity<CommitteeSummary> response = restTemplate.exchange(
                "/api/committees/" + committeeId + "/members", HttpMethod.POST,
                authedRequest(adminToken, new AddCommitteeMemberRequest(userId)), CommitteeSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

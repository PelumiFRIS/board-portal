package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fris.boardportal.actionitem.dto.ActionItemSummary;
import com.fris.boardportal.actionitem.dto.CreateActionItemRequest;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ActionItemNotificationFlowTest extends IntegrationTestSupport {

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void assigningAnActionItemEmailsOnlyTheAssignee() {
        AuthResponse admin = signup(uniqueEmail(), "Action Item Notify Org");
        String assigneeEmail = uniqueEmail();
        UUID assigneeId = createBoardMember(admin.accessToken(), assigneeEmail);
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());

        ResponseEntity<ActionItemSummary> response = restTemplate.exchange(
                "/api/action-items", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateActionItemRequest(meeting.id(), "Finalize the draft", null, assigneeId, null)),
                ActionItemSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // scheduling the meeting above already triggers its own notification, so two sends are expected here
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messageCaptor.capture());

        SimpleMailMessage sent = messageCaptor.getAllValues().stream()
                .filter(m -> m.getTo() != null)
                .findFirst()
                .orElseThrow();
        assertThat(sent.getSubject()).contains("Finalize the draft");
        assertThat(sent.getTo()).containsExactly(assigneeEmail);
        assertThat(sent.getBcc()).isNull();
    }

    @Test
    void assigningAnActionItemSucceedsEvenIfEmailSendingFails() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        AuthResponse admin = signup(uniqueEmail(), "Action Item Notify Failure Org");
        UUID assigneeId = createBoardMember(admin.accessToken(), uniqueEmail());
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/action-items", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateActionItemRequest(meeting.id(), "Finalize the draft", null, assigneeId, null)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private MeetingSummary scheduleMeeting(String adminToken) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest("Action Item Notify Meeting", null, null, start, null, null, defaultMeetingTypeId(adminToken))),
                MeetingSummary.class);
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
}

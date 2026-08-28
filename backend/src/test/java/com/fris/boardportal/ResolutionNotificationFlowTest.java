package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.resolution.dto.CreateResolutionRequest;
import com.fris.boardportal.resolution.dto.ResolutionSummary;
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

class ResolutionNotificationFlowTest extends IntegrationTestSupport {

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void openingAResolutionEmailsActiveMembersInBcc() {
        AuthResponse admin = signup(uniqueEmail(), "Resolution Notify Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        ResolutionSummary resolution = createResolution(admin.accessToken(), meeting.id());

        restTemplate.exchange("/api/resolutions/" + resolution.id() + "/open", HttpMethod.PATCH,
                authedRequest(admin.accessToken()), ResolutionSummary.class);

        // scheduling the meeting above already triggers its own notification, so two sends are expected here
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messageCaptor.capture());

        SimpleMailMessage sent = messageCaptor.getAllValues().stream()
                .filter(m -> m.getSubject() != null && m.getSubject().contains(resolution.title()))
                .findFirst()
                .orElseThrow();
        assertThat(sent.getBcc()).contains(admin.user().email(), memberEmail);
    }

    @Test
    void openingAResolutionSucceedsEvenIfEmailSendingFails() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        AuthResponse admin = signup(uniqueEmail(), "Resolution Notify Failure Org");
        MeetingSummary meeting = scheduleMeeting(admin.accessToken());
        ResolutionSummary resolution = createResolution(admin.accessToken(), meeting.id());

        ResponseEntity<ResolutionSummary> response = restTemplate.exchange(
                "/api/resolutions/" + resolution.id() + "/open", HttpMethod.PATCH,
                authedRequest(admin.accessToken()), ResolutionSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private MeetingSummary scheduleMeeting(String adminToken) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest("Resolution Notify Meeting", null, null, start, null, null)),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResolutionSummary createResolution(String adminToken, UUID meetingId) {
        ResponseEntity<ResolutionSummary> response = restTemplate.exchange(
                "/api/resolutions", HttpMethod.POST,
                authedRequest(adminToken, new CreateResolutionRequest(meetingId, "Approve FY26 budget", null)),
                ResolutionSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void createBoardMember(String adminToken, String email) {
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Member", email, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
    }
}

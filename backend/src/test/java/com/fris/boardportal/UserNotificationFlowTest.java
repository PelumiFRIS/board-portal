package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UpdateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class UserNotificationFlowTest extends IntegrationTestSupport {

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void adminEditingSomeoneElsesProfileEmailsThem() {
        AuthResponse admin = signup(uniqueEmail(), "Profile Notify Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);

        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users/" + member.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(), new UpdateUserRequest(null, null, "Treasurer", null, null)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly(memberEmail);
        assertThat(sent.getSubject()).contains("Your profile was updated");
        assertThat(sent.getText()).contains("title");
    }

    @Test
    void selfEditTriggersNoEmail() {
        AuthResponse admin = signup(uniqueEmail(), "Profile Self Notify Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users/" + member.id(), HttpMethod.PATCH,
                authedRequest(memberAuth.accessToken(), new UpdateUserRequest(null, null, "Treasurer", null, null)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void noOpUpdateTriggersNoEmail() {
        AuthResponse admin = signup(uniqueEmail(), "Profile Noop Notify Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);

        // title is already null, so re-sending role/status unchanged (both null = no-op) should not notify
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users/" + member.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(), new UpdateUserRequest(member.role(), member.status(), null, null, null)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void adminEditSucceedsEvenIfEmailSendingFails() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        AuthResponse admin = signup(uniqueEmail(), "Profile Notify Failure Org");
        UserSummary member = createBoardMember(admin.accessToken(), uniqueEmail());

        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users/" + member.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(), new UpdateUserRequest(null, null, "Treasurer", null, null)),
                UserSummary.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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

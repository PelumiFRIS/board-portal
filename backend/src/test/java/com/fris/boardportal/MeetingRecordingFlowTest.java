package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.meeting.recording.dto.MeetingRecordingSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class MeetingRecordingFlowTest extends IntegrationTestSupport {

    private static final byte[] AUDIO_BYTES = "fake webm audio bytes".getBytes(StandardCharsets.UTF_8);

    @Test
    void adminCanUploadListDownloadAndDeleteARecording() {
        AuthResponse admin = signup(uniqueEmail(), "Recording Org");
        UUID meetingId = scheduleMeeting(admin.accessToken());

        ResponseEntity<MeetingRecordingSummary> uploaded = uploadRecording(admin.accessToken(), meetingId);
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(uploaded.getBody().fileSize()).isEqualTo(AUDIO_BYTES.length);
        assertThat(uploaded.getBody().recordedByName()).isEqualTo("Ada Admin");

        ResponseEntity<MeetingRecordingSummary[]> list = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings", HttpMethod.GET,
                authedRequest(admin.accessToken()), MeetingRecordingSummary[].class);
        assertThat(list.getBody()).hasSize(1);

        UUID recordingId = uploaded.getBody().id();
        ResponseEntity<byte[]> content = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings/" + recordingId + "/content", HttpMethod.GET,
                authedRequest(admin.accessToken()), byte[].class);
        assertThat(content.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(content.getBody()).isEqualTo(AUDIO_BYTES);

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings/" + recordingId, HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<MeetingRecordingSummary[]> afterDelete = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings", HttpMethod.GET,
                authedRequest(admin.accessToken()), MeetingRecordingSummary[].class);
        assertThat(afterDelete.getBody()).isEmpty();
    }

    @Test
    void boardMemberCanViewButNotUploadOrDelete() {
        AuthResponse admin = signup(uniqueEmail(), "Recording Restricted Org");
        UUID meetingId = scheduleMeeting(admin.accessToken());
        UUID recordingId = uploadRecording(admin.accessToken(), meetingId).getBody().id();

        String memberEmail = uniqueEmail();
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateUserRequest("Board", "Member", memberEmail, "password123", Role.BOARD_MEMBER)),
                UserSummary.class);
        String memberToken = restTemplate.postForEntity(
                        "/api/auth/login", new LoginRequest(memberEmail, "password123"), AuthResponse.class)
                .getBody()
                .accessToken();

        ResponseEntity<MeetingRecordingSummary[]> list = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings", HttpMethod.GET,
                authedRequest(memberToken), MeetingRecordingSummary[].class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).hasSize(1);

        ResponseEntity<byte[]> content = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings/" + recordingId + "/content", HttpMethod.GET,
                authedRequest(memberToken), byte[].class);
        assertThat(content.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> uploadAttempt = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings", HttpMethod.POST,
                uploadRequest(memberToken), String.class);
        assertThat(uploadAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> deleteAttempt = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings/" + recordingId, HttpMethod.DELETE,
                authedRequest(memberToken), String.class);
        assertThat(deleteAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private UUID scheduleMeeting(String adminToken) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest("Recorded Meeting", null, null, start, null, null,
                        defaultMeetingTypeId(adminToken))),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    private ResponseEntity<MeetingRecordingSummary> uploadRecording(String token, UUID meetingId) {
        return restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings", HttpMethod.POST, uploadRequest(token),
                MeetingRecordingSummary.class);
    }

    private HttpEntity<MultiValueMap<String, Object>> uploadRequest(String token) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(AUDIO_BYTES) {
            @Override
            public String getFilename() {
                return "recording.webm";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}

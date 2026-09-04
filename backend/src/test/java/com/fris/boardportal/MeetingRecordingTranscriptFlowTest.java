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

/**
 * No OPENAI_API_KEY is configured in the test environment, so every call here exercises the
 * deterministic "not configured" error path rather than making a real network call to OpenAI.
 */
class MeetingRecordingTranscriptFlowTest extends IntegrationTestSupport {

    private static final byte[] AUDIO_BYTES = "fake webm audio bytes".getBytes(StandardCharsets.UTF_8);

    @Test
    void generatingATranscriptWithoutAnApiKeyFailsClearlyAndMarksTheRecordingFailed() {
        AuthResponse admin = signup(uniqueEmail(), "Transcript Org");
        UUID meetingId = scheduleMeeting(admin.accessToken());
        UUID recordingId = uploadRecording(admin.accessToken(), meetingId).getBody().id();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings/" + recordingId + "/transcript", HttpMethod.POST,
                authedRequest(admin.accessToken()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("OPENAI_API_KEY");

        ResponseEntity<MeetingRecordingSummary[]> list = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings", HttpMethod.GET,
                authedRequest(admin.accessToken()), MeetingRecordingSummary[].class);
        assertThat(list.getBody()[0].transcriptionStatus().toString()).isEqualTo("FAILED");
        assertThat(list.getBody()[0].transcriptText()).isNull();
    }

    @Test
    void boardMemberCannotGenerateATranscript() {
        AuthResponse admin = signup(uniqueEmail(), "Transcript Restricted Org");
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

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings/" + recordingId + "/transcript", HttpMethod.POST,
                authedRequest(memberToken), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                "/api/meetings/" + meetingId + "/recordings", HttpMethod.POST, request, MeetingRecordingSummary.class);
    }
}

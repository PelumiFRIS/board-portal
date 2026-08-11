package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.document.DocumentCategory;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingDetail;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
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

class DocumentFlowTest extends IntegrationTestSupport {

    private static final byte[] FILE_BYTES = "Q4 board pack contents".getBytes(StandardCharsets.UTF_8);

    @Test
    void adminCanUploadAndListDocument() {
        AuthResponse admin = signup(uniqueEmail(), "Docs Org");

        ResponseEntity<DocumentSummary> uploaded = uploadDocument(admin.accessToken(), "Q4 Board Pack",
                DocumentCategory.BOARD_PACK, null);
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(uploaded.getBody().fileSize()).isEqualTo(FILE_BYTES.length);

        ResponseEntity<DocumentSummary[]> list = restTemplate.exchange(
                "/api/documents", HttpMethod.GET, authedRequest(admin.accessToken()), DocumentSummary[].class);
        assertThat(list.getBody()).hasSize(1);
        assertThat(list.getBody()[0].title()).isEqualTo("Q4 Board Pack");
    }

    @Test
    void downloadReturnsByteIdenticalContent() {
        AuthResponse admin = signup(uniqueEmail(), "Download Org");
        DocumentSummary uploaded = uploadDocument(admin.accessToken(), "Policy", DocumentCategory.POLICY, null)
                .getBody();

        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/documents/" + uploaded.id() + "/content", HttpMethod.GET,
                authedRequest(admin.accessToken()), byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(FILE_BYTES);
    }

    @Test
    void nonAdminCannotUploadOrDelete() {
        AuthResponse admin = signup(uniqueEmail(), "Restricted Docs Org");
        String memberEmail = uniqueEmail();
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateUserRequest(
                        "Board", "Member", memberEmail, "password123", Role.BOARD_MEMBER)),
                Object.class);
        AuthResponse member = login(memberEmail);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/documents", HttpMethod.POST, uploadRequest(member.accessToken(), "Report", DocumentCategory.REPORT, null),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCannotAccessDocumentFromAnotherOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Docs Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Docs Org B");
        DocumentSummary orgBDoc = uploadDocument(orgBAdmin.accessToken(), "Org B Bylaw", DocumentCategory.BYLAW, null)
                .getBody();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/documents/" + orgBDoc.id(), HttpMethod.GET,
                authedRequest(orgAAdmin.accessToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void documentLinkedToMeetingAppearsInMeetingDetail() {
        AuthResponse admin = signup(uniqueEmail(), "Meeting Docs Org");

        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> meeting = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateMeetingRequest("Q1 Meeting", null, null, start, null)),
                MeetingSummary.class);

        uploadDocument(admin.accessToken(), "Q1 Board Pack", DocumentCategory.BOARD_PACK, meeting.getBody().id());

        ResponseEntity<MeetingDetail> detail = restTemplate.exchange(
                "/api/meetings/" + meeting.getBody().id(), HttpMethod.GET,
                authedRequest(admin.accessToken()), MeetingDetail.class);

        assertThat(detail.getBody().documents()).hasSize(1);
        assertThat(detail.getBody().documents().get(0).title()).isEqualTo("Q1 Board Pack");
    }

    private ResponseEntity<DocumentSummary> uploadDocument(String token, String title, DocumentCategory category,
            UUID meetingId) {
        return restTemplate.exchange(
                "/api/documents", HttpMethod.POST, uploadRequest(token, title, category, meetingId), DocumentSummary.class);
    }

    private HttpEntity<MultiValueMap<String, Object>> uploadRequest(String token, String title,
            DocumentCategory category, UUID meetingId) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(FILE_BYTES) {
            @Override
            public String getFilename() {
                return "document.txt";
            }
        });
        body.add("title", title);
        body.add("category", category.name());
        if (meetingId != null) {
            body.add("meetingId", meetingId.toString());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private AuthResponse login(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}

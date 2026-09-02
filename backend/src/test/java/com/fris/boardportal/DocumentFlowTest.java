package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.committee.dto.CommitteeSummary;
import com.fris.boardportal.committee.dto.CreateCommitteeRequest;
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
    void downloadingContentRecordsAnAuditEntryButViewingDetailDoesNot() {
        AuthResponse admin = signup(uniqueEmail(), "Download Audit Org");
        DocumentSummary uploaded = uploadDocument(admin.accessToken(), "Board Pack", DocumentCategory.BOARD_PACK, null)
                .getBody();

        restTemplate.exchange(
                "/api/documents/" + uploaded.id(), HttpMethod.GET,
                authedRequest(admin.accessToken()), String.class);

        ResponseEntity<byte[]> download = restTemplate.exchange(
                "/api/documents/" + uploaded.id() + "/content", HttpMethod.GET,
                authedRequest(admin.accessToken()), byte[].class);
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<AuditLogEntry[]> auditLog = restTemplate.exchange(
                "/api/audit-logs", HttpMethod.GET, authedRequest(admin.accessToken()), AuditLogEntry[].class);
        // exactly one DOCUMENT_DOWNLOADED entry — proves the earlier detail-view GET didn't also log one
        assertThat(auditLog.getBody())
                .filteredOn(e -> e.action() == AuditAction.DOCUMENT_DOWNLOADED)
                .hasSize(1)
                .allSatisfy(e -> assertThat(e.summary()).contains("Board Pack"));
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
                authedRequest(admin.accessToken(), new CreateMeetingRequest("Q1 Meeting", null, null, start, null, null, defaultMeetingTypeId(admin.accessToken()))),
                MeetingSummary.class);

        uploadDocument(admin.accessToken(), "Q1 Board Pack", DocumentCategory.BOARD_PACK, meeting.getBody().id());

        ResponseEntity<MeetingDetail> detail = restTemplate.exchange(
                "/api/meetings/" + meeting.getBody().id(), HttpMethod.GET,
                authedRequest(admin.accessToken()), MeetingDetail.class);

        assertThat(detail.getBody().documents()).hasSize(1);
        assertThat(detail.getBody().documents().get(0).title()).isEqualTo("Q1 Board Pack");
    }

    @Test
    void committeeIdFilterOnDocumentsList() {
        AuthResponse admin = signup(uniqueEmail(), "Committee Docs Org");
        CommitteeSummary committeeA = createCommittee(admin.accessToken(), "Audit Committee");
        CommitteeSummary committeeB = createCommittee(admin.accessToken(), "Risk Committee");

        uploadDocument(admin.accessToken(), "Audit Doc", DocumentCategory.REPORT, null, committeeA.id());
        uploadDocument(admin.accessToken(), "Org Wide Doc", DocumentCategory.OTHER, null);

        ResponseEntity<DocumentSummary[]> filteredA = restTemplate.exchange(
                "/api/documents?committeeId=" + committeeA.id(), HttpMethod.GET,
                authedRequest(admin.accessToken()), DocumentSummary[].class);
        assertThat(filteredA.getBody()).extracting(DocumentSummary::title).containsExactly("Audit Doc");
        assertThat(filteredA.getBody()[0].committeeId()).isEqualTo(committeeA.id());

        ResponseEntity<DocumentSummary[]> filteredB = restTemplate.exchange(
                "/api/documents?committeeId=" + committeeB.id(), HttpMethod.GET,
                authedRequest(admin.accessToken()), DocumentSummary[].class);
        assertThat(filteredB.getBody()).isEmpty();

        ResponseEntity<DocumentSummary[]> unfiltered = restTemplate.exchange(
                "/api/documents", HttpMethod.GET, authedRequest(admin.accessToken()), DocumentSummary[].class);
        assertThat(unfiltered.getBody()).hasSize(2);
        assertThat(unfiltered.getBody())
                .filteredOn(d -> d.title().equals("Org Wide Doc"))
                .allMatch(d -> d.committeeId() == null);
    }

    @Test
    void uploadingNewVersionSupersedesInListButPreservesHistory() {
        AuthResponse admin = signup(uniqueEmail(), "Version Org");
        DocumentSummary v1 = uploadDocument(admin.accessToken(), "Q4 Board Pack", DocumentCategory.BOARD_PACK, null)
                .getBody();
        assertThat(v1.versionNumber()).isEqualTo(1);
        assertThat(v1.rootDocumentId()).isEqualTo(v1.id());

        byte[] v2Bytes = "Q4 board pack contents, revised".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<DocumentSummary> v2Response = uploadNewVersion(admin.accessToken(), v1.id(), v2Bytes);
        assertThat(v2Response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentSummary v2 = v2Response.getBody();
        assertThat(v2.versionNumber()).isEqualTo(2);
        assertThat(v2.rootDocumentId()).isEqualTo(v1.id());
        assertThat(v2.title()).isEqualTo("Q4 Board Pack");
        assertThat(v2.category()).isEqualTo(DocumentCategory.BOARD_PACK);
        assertThat(v2.fileSize()).isEqualTo(v2Bytes.length);

        ResponseEntity<DocumentSummary[]> list = restTemplate.exchange(
                "/api/documents", HttpMethod.GET, authedRequest(admin.accessToken()), DocumentSummary[].class);
        assertThat(list.getBody()).hasSize(1);
        assertThat(list.getBody()[0].id()).isEqualTo(v2.id());
        assertThat(list.getBody()[0].versionNumber()).isEqualTo(2);

        ResponseEntity<DocumentSummary[]> versions = restTemplate.exchange(
                "/api/documents/" + v1.id() + "/versions", HttpMethod.GET,
                authedRequest(admin.accessToken()), DocumentSummary[].class);
        assertThat(versions.getBody()).extracting(DocumentSummary::versionNumber).containsExactly(2, 1);

        ResponseEntity<byte[]> v1Content = restTemplate.exchange(
                "/api/documents/" + v1.id() + "/content", HttpMethod.GET,
                authedRequest(admin.accessToken()), byte[].class);
        assertThat(v1Content.getBody()).isEqualTo(FILE_BYTES);

        ResponseEntity<byte[]> v2Content = restTemplate.exchange(
                "/api/documents/" + v2.id() + "/content", HttpMethod.GET,
                authedRequest(admin.accessToken()), byte[].class);
        assertThat(v2Content.getBody()).isEqualTo(v2Bytes);
    }

    @Test
    void nonAdminCannotUploadNewVersion() {
        AuthResponse admin = signup(uniqueEmail(), "Version Restricted Org");
        DocumentSummary v1 = uploadDocument(admin.accessToken(), "Policy", DocumentCategory.POLICY, null).getBody();
        String memberEmail = uniqueEmail();
        restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(admin.accessToken(),
                        new CreateUserRequest("Board", "Member", memberEmail, "password123", Role.BOARD_MEMBER)),
                Object.class);
        AuthResponse member = login(memberEmail);

        ResponseEntity<String> response = uploadNewVersion(member.accessToken(), v1.id(),
                "attempted".getBytes(StandardCharsets.UTF_8), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void signaturesDoNotCarryForwardToNewVersion() {
        AuthResponse admin = signup(uniqueEmail(), "Version Signature Org");
        DocumentSummary v1 = uploadDocument(admin.accessToken(), "Charter", DocumentCategory.CHARTER, null).getBody();

        ResponseEntity<DocumentSummary> signed = restTemplate.exchange(
                "/api/documents/" + v1.id() + "/sign", HttpMethod.POST,
                authedRequest(admin.accessToken()), DocumentSummary.class);
        assertThat(signed.getBody().signatureCount()).isEqualTo(1);

        DocumentSummary v2 = uploadNewVersion(admin.accessToken(), v1.id(),
                "revised charter".getBytes(StandardCharsets.UTF_8)).getBody();
        assertThat(v2.signatureCount()).isZero();
        assertThat(v2.signedByMe()).isFalse();
    }

    @Test
    void deletingLatestVersionRevealsThePreviousOneAsCurrent() {
        AuthResponse admin = signup(uniqueEmail(), "Version Delete Org");
        DocumentSummary v1 = uploadDocument(admin.accessToken(), "Bylaw", DocumentCategory.BYLAW, null).getBody();
        DocumentSummary v2 = uploadNewVersion(admin.accessToken(), v1.id(),
                "revised bylaw".getBytes(StandardCharsets.UTF_8)).getBody();

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/documents/" + v2.id(), HttpMethod.DELETE, authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<DocumentSummary[]> list = restTemplate.exchange(
                "/api/documents", HttpMethod.GET, authedRequest(admin.accessToken()), DocumentSummary[].class);
        assertThat(list.getBody()).hasSize(1);
        assertThat(list.getBody()[0].id()).isEqualTo(v1.id());
        assertThat(list.getBody()[0].versionNumber()).isEqualTo(1);
    }

    private ResponseEntity<DocumentSummary> uploadNewVersion(String token, UUID documentId, byte[] content) {
        return uploadNewVersion(token, documentId, content, DocumentSummary.class);
    }

    private <T> ResponseEntity<T> uploadNewVersion(String token, UUID documentId, byte[] content,
            Class<T> responseType) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "revised.txt";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/documents/" + documentId + "/versions", HttpMethod.POST,
                new HttpEntity<>(body, headers), responseType);
    }

    private CommitteeSummary createCommittee(String adminToken, String name) {
        ResponseEntity<CommitteeSummary> response = restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(adminToken, new CreateCommitteeRequest(name, null, null)), CommitteeSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResponseEntity<DocumentSummary> uploadDocument(String token, String title, DocumentCategory category,
            UUID meetingId) {
        return uploadDocument(token, title, category, meetingId, null);
    }

    private ResponseEntity<DocumentSummary> uploadDocument(String token, String title, DocumentCategory category,
            UUID meetingId, UUID committeeId) {
        return restTemplate.exchange(
                "/api/documents", HttpMethod.POST, uploadRequest(token, title, category, meetingId, committeeId),
                DocumentSummary.class);
    }

    private HttpEntity<MultiValueMap<String, Object>> uploadRequest(String token, String title,
            DocumentCategory category, UUID meetingId) {
        return uploadRequest(token, title, category, meetingId, null);
    }

    private HttpEntity<MultiValueMap<String, Object>> uploadRequest(String token, String title,
            DocumentCategory category, UUID meetingId, UUID committeeId) {
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
        if (committeeId != null) {
            body.add("committeeId", committeeId.toString());
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

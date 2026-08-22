package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.document.DocumentCategory;
import com.fris.boardportal.document.dto.DocumentDetail;
import com.fris.boardportal.document.dto.DocumentSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

class DocumentRetentionAndSignatureFlowTest extends IntegrationTestSupport {

    private static final byte[] FILE_BYTES = "Board policy contents".getBytes(StandardCharsets.UTF_8);

    @Test
    void adminCanSetAndClearRetentionButNonAdminCannot() {
        AuthResponse admin = signup(uniqueEmail(), "Retention Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);
        DocumentSummary doc = uploadDocument(admin.accessToken(), "Policy Doc").getBody();

        ResponseEntity<String> blocked = restTemplate.exchange(
                "/api/documents/" + doc.id() + "/retention", HttpMethod.PATCH,
                authedRequest(memberAuth.accessToken(), java.util.Map.of("retentionUntil", "2027-01-01")),
                String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<DocumentSummary> set = restTemplate.exchange(
                "/api/documents/" + doc.id() + "/retention", HttpMethod.PATCH,
                authedRequest(admin.accessToken(), java.util.Map.of("retentionUntil", "2027-01-01")),
                DocumentSummary.class);
        assertThat(set.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(set.getBody().retentionUntil()).isEqualTo(LocalDate.of(2027, 1, 1));

        ResponseEntity<DocumentSummary> cleared = restTemplate.exchange(
                "/api/documents/" + doc.id() + "/retention", HttpMethod.PATCH,
                authedRequest(admin.accessToken(), java.util.Collections.singletonMap("retentionUntil", null)),
                DocumentSummary.class);
        assertThat(cleared.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cleared.getBody().retentionUntil()).isNull();
    }

    @Test
    void anyMemberCanSignAndSigningTwiceIsANoOp() {
        AuthResponse admin = signup(uniqueEmail(), "Signing Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);
        DocumentSummary doc = uploadDocument(admin.accessToken(), "Minutes").getBody();
        assertThat(doc.signatureCount()).isEqualTo(0);
        assertThat(doc.signedByMe()).isFalse();

        ResponseEntity<DocumentSummary> firstSign = restTemplate.exchange(
                "/api/documents/" + doc.id() + "/sign", HttpMethod.POST,
                authedRequest(memberAuth.accessToken()), DocumentSummary.class);
        assertThat(firstSign.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstSign.getBody().signatureCount()).isEqualTo(1);
        assertThat(firstSign.getBody().signedByMe()).isTrue();

        ResponseEntity<DocumentSummary> secondSign = restTemplate.exchange(
                "/api/documents/" + doc.id() + "/sign", HttpMethod.POST,
                authedRequest(memberAuth.accessToken()), DocumentSummary.class);
        assertThat(secondSign.getBody().signatureCount()).isEqualTo(1);

        restTemplate.exchange(
                "/api/documents/" + doc.id() + "/sign", HttpMethod.POST,
                authedRequest(admin.accessToken()), DocumentSummary.class);

        ResponseEntity<DocumentDetail> detail = restTemplate.exchange(
                "/api/documents/" + doc.id(), HttpMethod.GET, authedRequest(admin.accessToken()), DocumentDetail.class);
        assertThat(detail.getBody().signatures()).hasSize(2);
        assertThat(detail.getBody().signatureCount()).isEqualTo(2);
    }

    @Test
    void retentionAndSignaturesAreScopedToOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Doc Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Doc Org B");
        DocumentSummary orgBDoc = uploadDocument(orgBAdmin.accessToken(), "Org B Doc").getBody();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/documents/" + orgBDoc.id() + "/retention", HttpMethod.PATCH,
                authedRequest(orgAAdmin.accessToken(), java.util.Map.of("retentionUntil", "2027-01-01")),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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

    private ResponseEntity<DocumentSummary> uploadDocument(String token, String title) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(FILE_BYTES) {
            @Override
            public String getFilename() {
                return "document.txt";
            }
        });
        body.add("title", title);
        body.add("category", DocumentCategory.POLICY.name());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        return restTemplate.exchange("/api/documents", HttpMethod.POST, request, DocumentSummary.class);
    }
}

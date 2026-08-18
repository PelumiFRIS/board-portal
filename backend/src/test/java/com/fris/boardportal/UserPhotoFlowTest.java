package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.nio.charset.StandardCharsets;
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

class UserPhotoFlowTest extends IntegrationTestSupport {

    private static final byte[] FAKE_IMAGE_BYTES = "not a real png but good enough for the test".getBytes(StandardCharsets.UTF_8);

    @Test
    void memberCanUploadReplaceAndDeleteOwnPhoto() {
        AuthResponse admin = signup(uniqueEmail(), "Photo Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<Void> uploaded = uploadPhoto(memberAuth.accessToken(), member.id(), "avatar.png");
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<byte[]> fetched = restTemplate.exchange(
                "/api/users/" + member.id() + "/photo", HttpMethod.GET,
                authedRequest(memberAuth.accessToken()), byte[].class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isEqualTo(FAKE_IMAGE_BYTES);

        // directory now reflects a photo timestamp
        ResponseEntity<UserSummary[]> directory = restTemplate.exchange(
                "/api/users/directory", HttpMethod.GET, authedRequest(admin.accessToken()), UserSummary[].class);
        assertThat(directory.getBody())
                .filteredOn(u -> u.id().equals(member.id()))
                .extracting(UserSummary::photoUpdatedAt)
                .allSatisfy(t -> assertThat(t).isNotNull());

        // replace it
        ResponseEntity<Void> replaced = uploadPhoto(memberAuth.accessToken(), member.id(), "avatar2.png");
        assertThat(replaced.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // delete it
        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/users/" + member.id() + "/photo", HttpMethod.DELETE,
                authedRequest(memberAuth.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> afterDelete = restTemplate.exchange(
                "/api/users/" + member.id() + "/photo", HttpMethod.GET,
                authedRequest(memberAuth.accessToken()), String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void memberCannotUploadPhotoForSomeoneElse() {
        AuthResponse admin = signup(uniqueEmail(), "Photo Boundary Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<Void> response = uploadPhoto(memberAuth.accessToken(), admin.user().id(), "avatar.png");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void nonImageUploadIsRejected() {
        AuthResponse admin = signup(uniqueEmail(), "Non Image Org");

        ResponseEntity<Void> response = uploadPhoto(admin.accessToken(), admin.user().id(), "notes.txt");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void adminCanUploadPhotoForAnyOrgMember() {
        AuthResponse admin = signup(uniqueEmail(), "Admin Photo Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);

        ResponseEntity<Void> response = uploadPhoto(admin.accessToken(), member.id(), "avatar.png");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void crossTenantPhotoAccessIsRejected() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Photo Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Photo Org B");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + orgBAdmin.user().id() + "/photo", HttpMethod.GET,
                authedRequest(orgAAdmin.accessToken()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Void> uploadPhoto(String token, java.util.UUID userId, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(FAKE_IMAGE_BYTES) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/users/" + userId + "/photo", HttpMethod.POST,
                new HttpEntity<>(body, headers), Void.class);
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

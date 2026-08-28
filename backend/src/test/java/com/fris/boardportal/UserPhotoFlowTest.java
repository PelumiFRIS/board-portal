package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
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

    private static final byte[] SQUARE_IMAGE_BYTES = generatePng(200, 200);
    private static final byte[] CORRUPT_BYTES = "not a real image".getBytes(StandardCharsets.UTF_8);

    @Test
    void memberCanUploadReplaceAndDeleteOwnPhoto() {
        AuthResponse admin = signup(uniqueEmail(), "Photo Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<Void> uploaded = uploadPhoto(memberAuth.accessToken(), member.id(), SQUARE_IMAGE_BYTES, "avatar.png");
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<byte[]> fetched = restTemplate.exchange(
                "/api/users/" + member.id() + "/photo", HttpMethod.GET,
                authedRequest(memberAuth.accessToken()), byte[].class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getHeaders().getContentType()).isNotNull();
        assertThat(fetched.getHeaders().getContentType().toString()).contains("image/jpeg");
        BufferedImage decoded = decode(fetched.getBody());
        assertThat(decoded.getWidth()).isEqualTo(decoded.getHeight());

        // directory now reflects a photo timestamp
        ResponseEntity<UserSummary[]> directory = restTemplate.exchange(
                "/api/users/directory", HttpMethod.GET, authedRequest(admin.accessToken()), UserSummary[].class);
        assertThat(directory.getBody())
                .filteredOn(u -> u.id().equals(member.id()))
                .extracting(UserSummary::photoUpdatedAt)
                .allSatisfy(t -> assertThat(t).isNotNull());

        // replace it
        ResponseEntity<Void> replaced = uploadPhoto(memberAuth.accessToken(), member.id(), SQUARE_IMAGE_BYTES, "avatar2.png");
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
    void wideAndTallPhotosAreCenterCroppedToSquareAndDownscaled() {
        AuthResponse admin = signup(uniqueEmail(), "Photo Crop Org");

        byte[] wideImage = generatePng(800, 400);
        uploadPhoto(admin.accessToken(), admin.user().id(), wideImage, "wide.png");
        BufferedImage wideResult = decode(fetchPhoto(admin.accessToken(), admin.user().id()));
        assertThat(wideResult.getWidth()).isEqualTo(wideResult.getHeight());
        assertThat(wideResult.getWidth()).isLessThanOrEqualTo(512);

        byte[] tallImage = generatePng(400, 800);
        uploadPhoto(admin.accessToken(), admin.user().id(), tallImage, "tall.png");
        BufferedImage tallResult = decode(fetchPhoto(admin.accessToken(), admin.user().id()));
        assertThat(tallResult.getWidth()).isEqualTo(tallResult.getHeight());
        assertThat(tallResult.getWidth()).isLessThanOrEqualTo(512);
    }

    @Test
    void smallPhotoIsNotUpscaled() {
        AuthResponse admin = signup(uniqueEmail(), "Photo Small Org");

        byte[] smallImage = generatePng(50, 50);
        uploadPhoto(admin.accessToken(), admin.user().id(), smallImage, "small.png");
        BufferedImage result = decode(fetchPhoto(admin.accessToken(), admin.user().id()));
        assertThat(result.getWidth()).isEqualTo(50);
        assertThat(result.getHeight()).isEqualTo(50);
    }

    @Test
    void corruptImageBytesAreRejected() {
        AuthResponse admin = signup(uniqueEmail(), "Photo Corrupt Org");

        ResponseEntity<Void> response = uploadPhoto(admin.accessToken(), admin.user().id(), CORRUPT_BYTES, "avatar.png");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void memberCannotUploadPhotoForSomeoneElse() {
        AuthResponse admin = signup(uniqueEmail(), "Photo Boundary Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResponseEntity<Void> response = uploadPhoto(memberAuth.accessToken(), admin.user().id(), SQUARE_IMAGE_BYTES, "avatar.png");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void nonImageUploadIsRejected() {
        AuthResponse admin = signup(uniqueEmail(), "Non Image Org");

        ResponseEntity<Void> response = uploadPhoto(admin.accessToken(), admin.user().id(), CORRUPT_BYTES, "notes.txt");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void adminCanUploadPhotoForAnyOrgMember() {
        AuthResponse admin = signup(uniqueEmail(), "Admin Photo Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);

        ResponseEntity<Void> response = uploadPhoto(admin.accessToken(), member.id(), SQUARE_IMAGE_BYTES, "avatar.png");
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

    private byte[] fetchPhoto(String token, java.util.UUID userId) {
        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/users/" + userId + "/photo", HttpMethod.GET, authedRequest(token), byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private BufferedImage decode(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] generatePng(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ResponseEntity<Void> uploadPhoto(String token, java.util.UUID userId, byte[] fileBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
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

package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.dto.BulkUserResult;
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

class BulkUserUploadFlowTest extends IntegrationTestSupport {

    @Test
    void adminCanBulkUploadMembersAndEachRowGetsItsOwnTemporaryPassword() {
        AuthResponse admin = signup(uniqueEmail(), "Bulk Upload Org");
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        String csv = "First Name,Last Name,Email,Role\n"
                + "Ada," + "Lovelace," + emailA + ",Board Member\n"
                + "Grace," + "Hopper," + emailB + ",Company Secretary\n";

        ResponseEntity<BulkUserResult[]> response = uploadCsv(admin.accessToken(), csv);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BulkUserResult[] results = response.getBody();
        assertThat(results).hasSize(2);
        assertThat(results[0].created()).isTrue();
        assertThat(results[0].email()).isEqualTo(emailA);
        assertThat(results[0].temporaryPassword()).isNotBlank();
        assertThat(results[1].created()).isTrue();
        assertThat(results[1].temporaryPassword()).isNotBlank();

        ResponseEntity<AuthResponse> loggedIn = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(emailA, results[0].temporaryPassword()), AuthResponse.class);
        assertThat(loggedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loggedIn.getBody().user().role().toString()).isEqualTo("BOARD_MEMBER");
    }

    @Test
    void badRowsFailIndependentlyWithoutBlockingGoodRows() {
        AuthResponse admin = signup(uniqueEmail(), "Bulk Upload Mixed Org");
        String goodEmail = uniqueEmail();
        String csv = "First Name,Last Name,Email,Role\n"
                + "Good,Row," + goodEmail + ",Board Member\n"
                + "Bad,Role," + uniqueEmail() + ",Astronaut\n"
                + ",Missing," + uniqueEmail() + ",Board Member\n"
                + "Dup,Email," + admin.user().email() + ",Board Member\n";

        ResponseEntity<BulkUserResult[]> response = uploadCsv(admin.accessToken(), csv);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BulkUserResult[] results = response.getBody();
        assertThat(results).hasSize(4);

        assertThat(results[0].created()).isTrue();
        assertThat(results[0].email()).isEqualTo(goodEmail);

        assertThat(results[1].created()).isFalse();
        assertThat(results[1].error()).contains("Unrecognized role");

        assertThat(results[2].created()).isFalse();
        assertThat(results[2].error()).contains("required");

        assertThat(results[3].created()).isFalse();
        assertThat(results[3].error()).contains("already exists");
    }

    @Test
    void nonAdminCannotBulkUpload() {
        AuthResponse admin = signup(uniqueEmail(), "Bulk Upload Forbidden Org");
        AuthResponse companySecretary = createCompanySecretaryAndLogin(admin.accessToken());
        String csv = "First Name,Last Name,Email,Role\nA,B," + uniqueEmail() + ",Board Member\n";

        ResponseEntity<String> response = uploadCsvRaw(companySecretary.accessToken(), csv);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingRequiredColumnsIsRejected() {
        AuthResponse admin = signup(uniqueEmail(), "Bulk Upload Bad Header Org");
        String csv = "Name,Email\nSomeone," + uniqueEmail() + "\n";

        ResponseEntity<String> response = uploadCsvRaw(admin.accessToken(), csv);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<BulkUserResult[]> uploadCsv(String token, String csv) {
        HttpEntity<MultiValueMap<String, Object>> request = csvRequest(token, csv);
        return restTemplate.exchange("/api/users/bulk", HttpMethod.POST, request, BulkUserResult[].class);
    }

    private ResponseEntity<String> uploadCsvRaw(String token, String csv) {
        HttpEntity<MultiValueMap<String, Object>> request = csvRequest(token, csv);
        return restTemplate.exchange("/api/users/bulk", HttpMethod.POST, request, String.class);
    }

    private HttpEntity<MultiValueMap<String, Object>> csvRequest(String token, String csv) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "members.csv";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}

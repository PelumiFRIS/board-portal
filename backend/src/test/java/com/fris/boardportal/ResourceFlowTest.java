package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.resource.ResourceCategory;
import com.fris.boardportal.resource.dto.CreateResourceRequest;
import com.fris.boardportal.resource.dto.ResourceSummary;
import com.fris.boardportal.resource.dto.UpdateResourceRequest;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ResourceFlowTest extends IntegrationTestSupport {

    @Test
    void nonAdminCanViewButNotMutate() {
        AuthResponse admin = signup(uniqueEmail(), "Resource View Org");
        String memberEmail = uniqueEmail();
        createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ResourceSummary resource = createResource(admin.accessToken(), ResourceCategory.ONBOARDING,
                "Welcome Guide", "Welcome to the board.");

        ResponseEntity<ResourceSummary[]> list = restTemplate.exchange(
                "/api/resources", HttpMethod.GET, authedRequest(memberAuth.accessToken()), ResourceSummary[].class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).extracting(ResourceSummary::title).containsExactly("Welcome Guide");

        ResponseEntity<String> create = restTemplate.exchange(
                "/api/resources", HttpMethod.POST,
                authedRequest(memberAuth.accessToken(),
                        new CreateResourceRequest(ResourceCategory.FAQ, "Sneaky FAQ", "Body")),
                String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> update = restTemplate.exchange(
                "/api/resources/" + resource.id(), HttpMethod.PATCH,
                authedRequest(memberAuth.accessToken(), new UpdateResourceRequest(null, "Renamed", null)),
                String.class);
        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> delete = restTemplate.exchange(
                "/api/resources/" + resource.id(), HttpMethod.DELETE,
                authedRequest(memberAuth.accessToken()), String.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanCreateEditAndDeleteResources() {
        AuthResponse admin = signup(uniqueEmail(), "Resource Admin Org");

        ResourceSummary resource = createResource(admin.accessToken(), ResourceCategory.GOVERNANCE_BEST_PRACTICES,
                "Conflict Policy", "Declare conflicts promptly.");
        assertThat(resource.category()).isEqualTo(ResourceCategory.GOVERNANCE_BEST_PRACTICES);

        ResponseEntity<ResourceSummary> updated = restTemplate.exchange(
                "/api/resources/" + resource.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(),
                        new UpdateResourceRequest(ResourceCategory.POLICIES_AND_PROCEDURES, "Conflict Policy (Revised)",
                                "Updated body")),
                ResourceSummary.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().category()).isEqualTo(ResourceCategory.POLICIES_AND_PROCEDURES);
        assertThat(updated.getBody().title()).isEqualTo("Conflict Policy (Revised)");
        assertThat(updated.getBody().body()).isEqualTo("Updated body");

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/resources/" + resource.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ResourceSummary[]> list = restTemplate.exchange(
                "/api/resources", HttpMethod.GET, authedRequest(admin.accessToken()), ResourceSummary[].class);
        assertThat(list.getBody()).isEmpty();
    }

    @Test
    void resourcesAreScopedToOrganization() {
        AuthResponse orgAAdmin = signup(uniqueEmail(), "Resource Org A");
        AuthResponse orgBAdmin = signup(uniqueEmail(), "Resource Org B");

        createResource(orgAAdmin.accessToken(), ResourceCategory.FAQ, "Org A FAQ", "Answer");
        ResourceSummary orgBResource = createResource(orgBAdmin.accessToken(), ResourceCategory.FAQ, "Org B FAQ", "Answer");

        ResponseEntity<ResourceSummary[]> orgAList = restTemplate.exchange(
                "/api/resources", HttpMethod.GET, authedRequest(orgAAdmin.accessToken()), ResourceSummary[].class);
        assertThat(orgAList.getBody()).extracting(ResourceSummary::title).containsExactly("Org A FAQ");

        ResponseEntity<String> crossOrgUpdate = restTemplate.exchange(
                "/api/resources/" + orgBResource.id(), HttpMethod.PATCH,
                authedRequest(orgAAdmin.accessToken(), new UpdateResourceRequest(null, "Hijacked", null)),
                String.class);
        assertThat(crossOrgUpdate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void mutationsAreAuditLoggedWithTheResourceTitle() {
        AuthResponse admin = signup(uniqueEmail(), "Resource Audit Org");

        ResourceSummary resource = createResource(admin.accessToken(), ResourceCategory.OTHER, "Handbook", "Body");
        restTemplate.exchange(
                "/api/resources/" + resource.id(), HttpMethod.PATCH,
                authedRequest(admin.accessToken(), new UpdateResourceRequest(null, null, "New body")),
                ResourceSummary.class);
        restTemplate.exchange(
                "/api/resources/" + resource.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), Void.class);

        ResponseEntity<AuditLogEntry[]> auditLog = restTemplate.exchange(
                "/api/audit-logs", HttpMethod.GET, authedRequest(admin.accessToken()), AuditLogEntry[].class);
        List<AuditLogEntry> entries = List.of(auditLog.getBody());

        assertThat(entries).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo(AuditAction.RESOURCE_CREATED);
            assertThat(e.summary()).contains("Handbook");
        });
        assertThat(entries).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo(AuditAction.RESOURCE_UPDATED);
            assertThat(e.summary()).contains("Handbook");
        });
        assertThat(entries).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo(AuditAction.RESOURCE_DELETED);
            assertThat(e.summary()).contains("Handbook");
        });
    }

    private ResourceSummary createResource(String adminToken, ResourceCategory category, String title, String body) {
        ResponseEntity<ResourceSummary> response = restTemplate.exchange(
                "/api/resources", HttpMethod.POST,
                authedRequest(adminToken, new CreateResourceRequest(category, title, body)),
                ResourceSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
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

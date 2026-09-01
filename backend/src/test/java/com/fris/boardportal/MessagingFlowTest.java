package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.messaging.dto.ConversationSummary;
import com.fris.boardportal.messaging.dto.CreateConversationRequest;
import com.fris.boardportal.messaging.dto.MessageDto;
import com.fris.boardportal.messaging.dto.SendMessageRequest;
import com.fris.boardportal.messaging.dto.UnreadCountResponse;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class MessagingFlowTest extends IntegrationTestSupport {

    @Test
    void startingADirectMessageMarksUnreadForRecipientOnlyAndReadingItClearsUnread() {
        AuthResponse admin = signup(uniqueEmail(), "Messaging DM Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ConversationSummary conversation = createConversation(admin.accessToken(),
                List.of(member.id()), "Hello there");

        assertThat(unreadCount(admin.accessToken())).isZero();
        assertThat(unreadCount(memberAuth.accessToken())).isEqualTo(1);

        List<MessageDto> messages = listMessages(memberAuth.accessToken(), conversation.id());
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).body()).isEqualTo("Hello there");

        assertThat(unreadCount(memberAuth.accessToken())).isZero();
    }

    @Test
    void secondMessageToSameUserReusesExistingConversation() {
        AuthResponse admin = signup(uniqueEmail(), "Messaging Dedup Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);

        ConversationSummary first = createConversation(admin.accessToken(), List.of(member.id()), "First message");
        ConversationSummary second = createConversation(admin.accessToken(), List.of(member.id()), "Second message");

        assertThat(second.id()).isEqualTo(first.id());

        List<ConversationSummary> adminConversations = listConversations(admin.accessToken());
        assertThat(adminConversations).hasSize(1);
        assertThat(adminConversations.get(0).lastMessagePreview()).isEqualTo("Second message");
    }

    @Test
    void groupConversationIncludesEveryRecipientAndEachSeesIt() {
        AuthResponse admin = signup(uniqueEmail(), "Messaging Group Org");
        UserSummary memberOne = createBoardMember(admin.accessToken(), uniqueEmail());
        UserSummary memberTwo = createBoardMember(admin.accessToken(), uniqueEmail());
        AuthResponse memberOneAuth = login(memberOne.email());
        AuthResponse memberTwoAuth = login(memberTwo.email());

        ConversationSummary group = createConversation(admin.accessToken(),
                List.of(memberOne.id(), memberTwo.id()), "Welcome to the group", "Governance Committee");

        assertThat(group.isGroup()).isTrue();
        assertThat(group.title()).isEqualTo("Governance Committee");
        assertThat(group.participants()).hasSize(3);
        assertThat(group.participants()).extracting(p -> p.email()).contains(memberOne.email(), memberTwo.email());

        assertThat(listConversations(memberOneAuth.accessToken())).hasSize(1);
        assertThat(listConversations(memberTwoAuth.accessToken())).hasSize(1);
    }

    @Test
    void nonParticipantCannotReadOrPostToAConversation() {
        AuthResponse admin = signup(uniqueEmail(), "Messaging Access Org");
        UserSummary member = createBoardMember(admin.accessToken(), uniqueEmail());
        UserSummary outsider = createBoardMember(admin.accessToken(), uniqueEmail());
        AuthResponse outsiderAuth = login(outsider.email());

        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "Private chat");

        ResponseEntity<String> readAttempt = restTemplate.exchange(
                "/api/conversations/" + conversation.id() + "/messages", HttpMethod.GET,
                authedRequest(outsiderAuth.accessToken()), String.class);
        assertThat(readAttempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> postAttempt = restTemplate.exchange(
                "/api/conversations/" + conversation.id() + "/messages", HttpMethod.POST,
                authedRequest(outsiderAuth.accessToken(), new SendMessageRequest("Sneaky")), String.class);
        assertThat(postAttempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unreadCountSumsAcrossMultipleConversations() {
        AuthResponse admin = signup(uniqueEmail(), "Messaging Sum Org");
        UserSummary memberOne = createBoardMember(admin.accessToken(), uniqueEmail());
        UserSummary memberTwo = createBoardMember(admin.accessToken(), uniqueEmail());
        AuthResponse memberOneAuth = login(memberOne.email());
        AuthResponse memberTwoAuth = login(memberTwo.email());

        createConversation(admin.accessToken(), List.of(memberOne.id()), "Direct message to member one");
        createConversation(admin.accessToken(), List.of(memberOne.id(), memberTwo.id()), "Group ping", "Both");

        // sender's own unread count stays at zero across both conversations
        assertThat(unreadCount(admin.accessToken())).isZero();
        // memberOne is in both the DM and the group -- unread sums across the two
        assertThat(unreadCount(memberOneAuth.accessToken())).isEqualTo(2);
        // memberTwo is only in the group
        assertThat(unreadCount(memberTwoAuth.accessToken())).isEqualTo(1);
    }

    @Test
    void participantFromAnotherOrganizationIsRejected() {
        AuthResponse admin = signup(uniqueEmail(), "Messaging Org A");
        AuthResponse otherOrgAdmin = signup(uniqueEmail(), "Messaging Org B");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/conversations", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateConversationRequest(
                        List.of(otherOrgAdmin.user().id()), "Hi from another org", null)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void auditSummaryNamesParticipantsButNeverIncludesTheMessageBody() {
        AuthResponse admin = signup(uniqueEmail(), "Messaging Audit Org");
        UserSummary member = createBoardMember(admin.accessToken(), uniqueEmail());

        createConversation(admin.accessToken(), List.of(member.id()), "A very private secret");

        ResponseEntity<AuditLogEntry[]> auditLog = restTemplate.exchange(
                "/api/audit-logs", HttpMethod.GET, authedRequest(admin.accessToken()), AuditLogEntry[].class);

        List<AuditLogEntry> messageEntries = List.of(auditLog.getBody()).stream()
                .filter(e -> e.action() == AuditAction.MESSAGE_SENT)
                .toList();
        assertThat(messageEntries).hasSize(1);
        assertThat(messageEntries.get(0).summary())
                .contains(member.firstName())
                .doesNotContain("A very private secret");
    }

    private ConversationSummary createConversation(String token, List<UUID> participantIds, String initialMessage) {
        return createConversation(token, participantIds, initialMessage, null);
    }

    private ConversationSummary createConversation(String token, List<UUID> participantIds, String initialMessage,
            String title) {
        ResponseEntity<ConversationSummary> response = restTemplate.exchange(
                "/api/conversations", HttpMethod.POST,
                authedRequest(token, new CreateConversationRequest(participantIds, initialMessage, title)),
                ConversationSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private List<ConversationSummary> listConversations(String token) {
        ResponseEntity<ConversationSummary[]> response = restTemplate.exchange(
                "/api/conversations", HttpMethod.GET, authedRequest(token), ConversationSummary[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return List.of(response.getBody());
    }

    private List<MessageDto> listMessages(String token, UUID conversationId) {
        ResponseEntity<MessageDto[]> response = restTemplate.exchange(
                "/api/conversations/" + conversationId + "/messages", HttpMethod.GET, authedRequest(token),
                MessageDto[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return List.of(response.getBody());
    }

    private long unreadCount(String token) {
        ResponseEntity<UnreadCountResponse> response = restTemplate.exchange(
                "/api/conversations/unread-count", HttpMethod.GET, authedRequest(token), UnreadCountResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().count();
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

package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.dto.AuditLogEntry;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.auth.dto.LoginRequest;
import com.fris.boardportal.messaging.dto.ConversationSummary;
import com.fris.boardportal.messaging.dto.CreateConversationRequest;
import com.fris.boardportal.messaging.dto.MessageDto;
import com.fris.boardportal.messaging.dto.ParticipantSummary;
import com.fris.boardportal.messaging.dto.ReactionSummary;
import com.fris.boardportal.messaging.dto.SendMessageRequest;
import com.fris.boardportal.messaging.dto.ToggleReactionRequest;
import com.fris.boardportal.messaging.dto.UnreadCountResponse;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import java.util.List;
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

    @Test
    void readingMessagesMarksSenderAsSeenByTheReader() {
        AuthResponse admin = signup(uniqueEmail(), "Seen By Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "Please review");

        // Before the member has opened the conversation, the sender's copy shows no one has seen it yet.
        List<MessageDto> beforeRead = listMessages(admin.accessToken(), conversation.id());
        assertThat(beforeRead.get(0).seenBy()).isEmpty();

        // Member opens the conversation (listMessages bumps their lastReadAt).
        listMessages(memberAuth.accessToken(), conversation.id());

        List<MessageDto> afterRead = listMessages(admin.accessToken(), conversation.id());
        assertThat(afterRead.get(0).seenBy()).extracting(p -> p.email()).containsExactly(memberEmail);
    }

    @Test
    void togglingAReactionAddsThenRemovesIt() {
        AuthResponse admin = signup(uniqueEmail(), "Reactions Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "Nice work");
        UUID messageId = listMessages(admin.accessToken(), conversation.id()).get(0).id();

        MessageDto reacted = toggleReaction(memberAuth.accessToken(), conversation.id(), messageId, "👍");
        assertThat(reacted.reactions()).hasSize(1);
        ReactionSummary summary = reacted.reactions().get(0);
        assertThat(summary.emoji()).isEqualTo("👍");
        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.reactedByMe()).isTrue();

        // The sender's own view of the message shows the reaction too, but reactedByMe is false for them.
        List<MessageDto> senderView = listMessages(admin.accessToken(), conversation.id());
        assertThat(senderView.get(0).reactions().get(0).reactedByMe()).isFalse();

        MessageDto unreacted = toggleReaction(memberAuth.accessToken(), conversation.id(), messageId, "👍");
        assertThat(unreacted.reactions()).isEmpty();
    }

    @Test
    void reactionWithUnsupportedEmojiIsRejected() {
        AuthResponse admin = signup(uniqueEmail(), "Bad Reaction Org");
        UserSummary member = createBoardMember(admin.accessToken(), uniqueEmail());
        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "Hi");
        UUID messageId = listMessages(admin.accessToken(), conversation.id()).get(0).id();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/conversations/" + conversation.id() + "/messages/" + messageId + "/reactions",
                HttpMethod.POST, authedRequest(admin.accessToken(), new ToggleReactionRequest("🍕")), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mutingAConversationExcludesItFromUnreadCountButNotFromTheConversationListing() {
        AuthResponse admin = signup(uniqueEmail(), "Mute Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "Ping");
        assertThat(unreadCount(memberAuth.accessToken())).isEqualTo(1);

        ConversationSummary muted = toggleMute(memberAuth.accessToken(), conversation.id());
        assertThat(muted.muted()).isTrue();
        assertThat(unreadCount(memberAuth.accessToken())).isZero();

        // Mute only silences the notification bell -- the Messages list itself still shows the real unread count.
        List<ConversationSummary> stillListed = listConversations(memberAuth.accessToken());
        assertThat(stillListed.get(0).unreadCount()).isEqualTo(1);

        ConversationSummary unmuted = toggleMute(memberAuth.accessToken(), conversation.id());
        assertThat(unmuted.muted()).isFalse();
        assertThat(unreadCount(memberAuth.accessToken())).isEqualTo(1);
    }

    @Test
    void togglingImportantFlagsAndUnflagsAMessageForEveryParticipant() {
        AuthResponse admin = signup(uniqueEmail(), "Important Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "Key decision");
        UUID messageId = listMessages(admin.accessToken(), conversation.id()).get(0).id();

        MessageDto flagged = toggleImportant(memberAuth.accessToken(), conversation.id(), messageId);
        assertThat(flagged.important()).isTrue();

        // Visible to every participant, not just the one who flagged it.
        List<MessageDto> senderView = listMessages(admin.accessToken(), conversation.id());
        assertThat(senderView.get(0).important()).isTrue();

        MessageDto unflagged = toggleImportant(memberAuth.accessToken(), conversation.id(), messageId);
        assertThat(unflagged.important()).isFalse();
    }

    @Test
    void participantBecomesOnlineAfterMakingAnAuthenticatedRequest() {
        AuthResponse admin = signup(uniqueEmail(), "Presence Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "Hi");

        // Before the member has made any authenticated call, they show up as offline.
        ParticipantSummary before = participant(listConversations(admin.accessToken()).get(0), memberEmail);
        assertThat(before.online()).isFalse();

        // One authenticated request from the member touches their presence.
        listMessages(memberAuth.accessToken(), conversation.id());

        ParticipantSummary after = participant(listConversations(admin.accessToken()).get(0), memberEmail);
        assertThat(after.online()).isTrue();
    }

    @Test
    void uploadingAnAttachmentIsReturnedInTheAttachmentAndDownloadsWithTheSameBytes() {
        AuthResponse admin = signup(uniqueEmail(), "Attachments Org");
        String memberEmail = uniqueEmail();
        UserSummary member = createBoardMember(admin.accessToken(), memberEmail);
        AuthResponse memberAuth = login(memberEmail);

        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "See attached");
        UUID messageId = listMessages(admin.accessToken(), conversation.id()).get(0).id();
        byte[] content = "board pack excerpt".getBytes();

        MessageDto uploaded = uploadAttachment(memberAuth.accessToken(), conversation.id(), messageId, content, "notes.txt");
        assertThat(uploaded.attachment()).isNotNull();
        assertThat(uploaded.attachment().fileName()).isEqualTo("notes.txt");
        assertThat(uploaded.attachment().fileSize()).isEqualTo(content.length);

        // Visible to every participant, not just the uploader.
        List<MessageDto> senderView = listMessages(admin.accessToken(), conversation.id());
        assertThat(senderView.get(0).attachment()).isNotNull();
        assertThat(senderView.get(0).attachment().fileName()).isEqualTo("notes.txt");

        ResponseEntity<byte[]> downloaded = restTemplate.exchange(
                "/api/conversations/" + conversation.id() + "/messages/" + messageId + "/attachment",
                HttpMethod.GET, authedRequest(admin.accessToken()), byte[].class);
        assertThat(downloaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloaded.getBody()).isEqualTo(content);
    }

    @Test
    void nonParticipantCannotUploadOrDownloadAnAttachment() {
        AuthResponse admin = signup(uniqueEmail(), "Attachments Access Org");
        UserSummary member = createBoardMember(admin.accessToken(), uniqueEmail());
        UserSummary outsider = createBoardMember(admin.accessToken(), uniqueEmail());
        AuthResponse outsiderAuth = login(outsider.email());

        ConversationSummary conversation = createConversation(admin.accessToken(), List.of(member.id()), "Private");
        UUID messageId = listMessages(admin.accessToken(), conversation.id()).get(0).id();

        ResponseEntity<String> uploadAttempt = restTemplate.exchange(
                "/api/conversations/" + conversation.id() + "/messages/" + messageId + "/attachment",
                HttpMethod.POST, attachmentUploadRequest(outsiderAuth.accessToken(), "sneaky".getBytes(), "x.txt"),
                String.class);
        assertThat(uploadAttempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> downloadAttempt = restTemplate.exchange(
                "/api/conversations/" + conversation.id() + "/messages/" + messageId + "/attachment",
                HttpMethod.GET, authedRequest(outsiderAuth.accessToken()), String.class);
        assertThat(downloadAttempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private MessageDto uploadAttachment(String token, UUID conversationId, UUID messageId, byte[] content, String fileName) {
        ResponseEntity<MessageDto> response = restTemplate.exchange(
                "/api/conversations/" + conversationId + "/messages/" + messageId + "/attachment",
                HttpMethod.POST, attachmentUploadRequest(token, content, fileName), MessageDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private HttpEntity<MultiValueMap<String, Object>> attachmentUploadRequest(String token, byte[] content, String fileName) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private ParticipantSummary participant(ConversationSummary conversation, String email) {
        return conversation.participants().stream().filter(p -> p.email().equals(email)).findFirst().orElseThrow();
    }

    private ConversationSummary toggleMute(String token, UUID conversationId) {
        ResponseEntity<ConversationSummary> response = restTemplate.exchange(
                "/api/conversations/" + conversationId + "/mute",
                HttpMethod.POST, authedRequest(token), ConversationSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private MessageDto toggleImportant(String token, UUID conversationId, UUID messageId) {
        ResponseEntity<MessageDto> response = restTemplate.exchange(
                "/api/conversations/" + conversationId + "/messages/" + messageId + "/important",
                HttpMethod.POST, authedRequest(token), MessageDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private MessageDto toggleReaction(String token, UUID conversationId, UUID messageId, String emoji) {
        ResponseEntity<MessageDto> response = restTemplate.exchange(
                "/api/conversations/" + conversationId + "/messages/" + messageId + "/reactions",
                HttpMethod.POST, authedRequest(token, new ToggleReactionRequest(emoji)), MessageDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
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

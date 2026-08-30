package com.fris.boardportal.messaging;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.messaging.dto.ConversationSummary;
import com.fris.boardportal.messaging.dto.CreateConversationRequest;
import com.fris.boardportal.messaging.dto.MessageDto;
import com.fris.boardportal.messaging.dto.ParticipantSummary;
import com.fris.boardportal.messaging.dto.SendMessageRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import com.fris.boardportal.user.UserStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ConversationService(ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository, MessageRepository messageRepository,
            UserRepository userRepository, AuditLogService auditLogService) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<ConversationSummary> listConversations(AppUserPrincipal principal) {
        return participantRepository.findByUserId(principal.getUserId()).stream()
                .map(this::toSummary)
                .filter(s -> s != null)
                .sorted(Comparator.comparing(ConversationSummary::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public long unreadCount(AppUserPrincipal principal) {
        return participantRepository.findByUserId(principal.getUserId()).stream()
                .mapToLong(p -> messageRepository.countByConversationIdAndCreatedAtAfter(
                        p.getConversationId(), p.getLastReadAt() != null ? p.getLastReadAt() : Instant.EPOCH))
                .sum();
    }

    @Transactional
    public ConversationSummary createConversation(AppUserPrincipal principal, CreateConversationRequest request) {
        UUID orgId = principal.getOrganizationId();
        Set<UUID> recipientIds = new LinkedHashSet<>(request.participantIds());
        recipientIds.remove(principal.getUserId());
        if (recipientIds.isEmpty()) {
            throw ApiException.badRequest("Select at least one other recipient");
        }
        for (UUID recipientId : recipientIds) {
            User recipient = userRepository.findByIdAndOrganizationId(recipientId, orgId)
                    .orElseThrow(() -> ApiException.badRequest("Recipient not found in your organization"));
            if (recipient.getStatus() != UserStatus.ACTIVE) {
                throw ApiException.badRequest("Cannot message a disabled member");
            }
        }

        boolean isGroup = recipientIds.size() > 1;
        Conversation conversation;
        boolean reused = false;
        if (!isGroup) {
            Conversation existing = findExistingDirectConversation(principal.getUserId(), recipientIds.iterator().next());
            if (existing != null) {
                conversation = existing;
                reused = true;
            } else {
                conversation = createConversationRecord(orgId, false, request.title(), principal, recipientIds);
            }
        } else {
            conversation = createConversationRecord(orgId, true, request.title(), principal, recipientIds);
        }

        Message message = postMessage(principal, conversation, request.initialMessage());

        List<UUID> otherParticipantIds = participantRepository.findByConversationId(conversation.getId()).stream()
                .map(ConversationParticipant::getUserId)
                .filter(id -> !id.equals(principal.getUserId()))
                .toList();
        String verb = reused ? "Sent a message in a conversation with " : "Started a conversation with ";
        auditLogService.record(principal, AuditAction.MESSAGE_SENT, AuditEntityType.CONVERSATION, conversation.getId(),
                verb + participantNames(otherParticipantIds));

        return toSummary(conversation, message, principal);
    }

    @Transactional
    public MessageDto sendMessage(AppUserPrincipal principal, UUID conversationId, SendMessageRequest request) {
        Conversation conversation = conversationRepository
                .findByIdAndOrganizationId(conversationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        participantRepository.findByConversationIdAndUserId(conversationId, principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));

        Message message = postMessage(principal, conversation, request.body());

        List<UUID> otherParticipantIds = participantRepository.findByConversationId(conversationId).stream()
                .map(ConversationParticipant::getUserId)
                .filter(id -> !id.equals(principal.getUserId()))
                .toList();
        auditLogService.record(principal, AuditAction.MESSAGE_SENT, AuditEntityType.CONVERSATION, conversation.getId(),
                "Sent a message in a conversation with " + participantNames(otherParticipantIds));

        return MessageDto.from(message);
    }

    @Transactional
    public List<MessageDto> listMessages(AppUserPrincipal principal, UUID conversationId) {
        conversationRepository.findByIdAndOrganizationId(conversationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));

        participant.setLastReadAt(Instant.now());
        participantRepository.save(participant);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(MessageDto::from)
                .toList();
    }

    private Conversation createConversationRecord(UUID orgId, boolean isGroup, String title,
            AppUserPrincipal principal, Set<UUID> recipientIds) {
        Conversation conversation = Conversation.create(orgId, isGroup, title, principal.getUserId());
        conversationRepository.save(conversation);
        participantRepository.save(ConversationParticipant.create(conversation.getId(), principal.getUserId(), Instant.now()));
        for (UUID recipientId : recipientIds) {
            participantRepository.save(ConversationParticipant.create(conversation.getId(), recipientId, null));
        }
        return conversation;
    }

    private Message postMessage(AppUserPrincipal principal, Conversation conversation, String body) {
        Message message = Message.create(conversation.getId(), principal.getUserId(), senderName(principal), body);
        messageRepository.save(message);

        ConversationParticipant self = participantRepository
                .findByConversationIdAndUserId(conversation.getId(), principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        self.setLastReadAt(message.getCreatedAt());
        participantRepository.save(self);

        return message;
    }

    private Conversation findExistingDirectConversation(UUID userId, UUID otherUserId) {
        for (ConversationParticipant p : participantRepository.findByUserId(userId)) {
            Conversation conversation = conversationRepository.findById(p.getConversationId()).orElse(null);
            if (conversation == null || conversation.isGroup()) {
                continue;
            }
            List<ConversationParticipant> members = participantRepository.findByConversationId(conversation.getId());
            if (members.size() == 2 && members.stream().anyMatch(m -> m.getUserId().equals(otherUserId))) {
                return conversation;
            }
        }
        return null;
    }

    private ConversationSummary toSummary(ConversationParticipant myParticipation) {
        Conversation conversation = conversationRepository.findById(myParticipation.getConversationId()).orElse(null);
        if (conversation == null) {
            return null;
        }
        Message lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);
        return buildSummary(conversation, myParticipation, lastMessage);
    }

    private ConversationSummary toSummary(Conversation conversation, Message lastMessage, AppUserPrincipal principal) {
        ConversationParticipant myParticipation = participantRepository
                .findByConversationIdAndUserId(conversation.getId(), principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        return buildSummary(conversation, myParticipation, lastMessage);
    }

    private ConversationSummary buildSummary(Conversation conversation, ConversationParticipant myParticipation,
            Message lastMessage) {
        List<ParticipantSummary> participants = participantRepository.findByConversationId(conversation.getId()).stream()
                .map(p -> userRepository.findById(p.getUserId())
                        .map(u -> new ParticipantSummary(u.getId(), u.getFirstName(), u.getLastName()))
                        .orElse(null))
                .filter(p -> p != null)
                .toList();

        long unread = messageRepository.countByConversationIdAndCreatedAtAfter(conversation.getId(),
                myParticipation.getLastReadAt() != null ? myParticipation.getLastReadAt() : Instant.EPOCH);

        return new ConversationSummary(conversation.getId(), conversation.isGroup(), conversation.getTitle(),
                participants, lastMessage != null ? lastMessage.getBody() : null,
                lastMessage != null ? lastMessage.getCreatedAt() : null, unread);
    }

    private String senderName(AppUserPrincipal principal) {
        return userRepository.findById(principal.getUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(principal.getUsername());
    }

    private String participantNames(Collection<UUID> userIds) {
        return userIds.stream()
                .map(id -> userRepository.findById(id)
                        .map(u -> u.getFirstName() + " " + u.getLastName())
                        .orElse("a member"))
                .collect(Collectors.joining(", "));
    }
}

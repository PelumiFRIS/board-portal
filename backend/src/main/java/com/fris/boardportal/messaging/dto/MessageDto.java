package com.fris.boardportal.messaging.dto;

import com.fris.boardportal.messaging.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String body,
        Instant createdAt,
        List<ParticipantSummary> seenBy,
        List<ReactionSummary> reactions,
        boolean important,
        AttachmentSummary attachment) {

    public static MessageDto from(Message message, List<ParticipantSummary> seenBy, List<ReactionSummary> reactions) {
        return from(message, seenBy, reactions, null);
    }

    public static MessageDto from(Message message, List<ParticipantSummary> seenBy, List<ReactionSummary> reactions,
            AttachmentSummary attachment) {
        return new MessageDto(message.getId(), message.getConversationId(), message.getSenderId(),
                message.getSenderName(), message.getBody(), message.getCreatedAt(), seenBy, reactions,
                message.isImportant(), attachment);
    }
}

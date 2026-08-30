package com.fris.boardportal.messaging.dto;

import com.fris.boardportal.messaging.Message;
import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String body,
        Instant createdAt) {

    public static MessageDto from(Message message) {
        return new MessageDto(message.getId(), message.getConversationId(), message.getSenderId(),
                message.getSenderName(), message.getBody(), message.getCreatedAt());
    }
}

package com.fris.boardportal.messaging.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationSummary(
        UUID id,
        boolean isGroup,
        String title,
        List<ParticipantSummary> participants,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount) {
}

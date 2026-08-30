package com.fris.boardportal.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CreateConversationRequest(
        @NotEmpty List<UUID> participantIds,
        @NotBlank String initialMessage,
        String title) {
}

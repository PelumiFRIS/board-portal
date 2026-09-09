package com.fris.boardportal.messaging.dto;

import jakarta.validation.constraints.NotBlank;

public record ToggleReactionRequest(@NotBlank String emoji) {
}

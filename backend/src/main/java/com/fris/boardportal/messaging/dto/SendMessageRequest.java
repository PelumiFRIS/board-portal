package com.fris.boardportal.messaging.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(@NotBlank String body) {
}

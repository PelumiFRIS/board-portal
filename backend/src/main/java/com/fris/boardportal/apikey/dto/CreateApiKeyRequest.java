package com.fris.boardportal.apikey.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateApiKeyRequest(@NotBlank String name) {
}

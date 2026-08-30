package com.fris.boardportal.resource.dto;

import com.fris.boardportal.resource.ResourceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateResourceRequest(
        @NotNull ResourceCategory category,
        @NotBlank String title,
        @NotBlank String body) {
}

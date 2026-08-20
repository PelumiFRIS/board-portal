package com.fris.boardportal.committee.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommitteeRequest(@NotBlank String name, String description) {
}

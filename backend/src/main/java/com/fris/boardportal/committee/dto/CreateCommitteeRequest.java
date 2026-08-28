package com.fris.boardportal.committee.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateCommitteeRequest(@NotBlank String name, String description, UUID parentCommitteeId) {
}

package com.fris.boardportal.meeting.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAgendaItemRequest(@NotBlank String title, String description, Integer position) {
}

package com.fris.boardportal.meeting.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMeetingTypeRequest(@NotBlank String name) {
}

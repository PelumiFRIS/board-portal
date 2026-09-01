package com.fris.boardportal.messaging.dto;

import java.util.UUID;

public record ParticipantSummary(UUID userId, String firstName, String lastName, String email) {
}

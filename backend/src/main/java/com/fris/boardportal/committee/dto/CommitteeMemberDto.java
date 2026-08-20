package com.fris.boardportal.committee.dto;

import java.util.UUID;

public record CommitteeMemberDto(
        UUID userId,
        String firstName,
        String lastName,
        boolean isChair) {
}

package com.fris.boardportal.user.dto;

import java.util.UUID;

public record MemberCommitteeSummary(UUID committeeId, String committeeName, boolean isChair) {
}

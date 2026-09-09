package com.fris.boardportal.messaging.dto;

public record ReactionSummary(String emoji, long count, boolean reactedByMe) {
}

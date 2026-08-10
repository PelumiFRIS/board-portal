package com.fris.boardportal.auth.dto;

import com.fris.boardportal.user.dto.UserSummary;

public record AuthResponse(String accessToken, UserSummary user) {
}

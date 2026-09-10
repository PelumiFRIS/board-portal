package com.fris.boardportal.organization.dto;

import com.fris.boardportal.user.dto.UserSummary;

public record OrganizationOnboardResponse(UserSummary admin, String temporaryPassword) {
}

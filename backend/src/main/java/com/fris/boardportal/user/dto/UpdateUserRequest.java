package com.fris.boardportal.user.dto;

import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.UserStatus;

public record UpdateUserRequest(
        Role role,
        UserStatus status,
        String title,
        String phone,
        String bio) {
}

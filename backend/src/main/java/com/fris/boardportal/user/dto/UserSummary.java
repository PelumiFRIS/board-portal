package com.fris.boardportal.user.dto;

import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role,
        UserStatus status,
        UUID organizationId,
        String organizationName,
        String title,
        String phone,
        String bio,
        String committees,
        Instant photoUpdatedAt) {

    public static UserSummary from(User user, String organizationName, Instant photoUpdatedAt) {
        return new UserSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getOrganizationId(),
                organizationName,
                user.getTitle(),
                user.getPhone(),
                user.getBio(),
                user.getCommittees(),
                photoUpdatedAt);
    }
}

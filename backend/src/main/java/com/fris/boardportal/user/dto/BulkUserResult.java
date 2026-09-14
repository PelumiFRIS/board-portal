package com.fris.boardportal.user.dto;

import com.fris.boardportal.user.Role;

public record BulkUserResult(
        int row,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean created,
        String temporaryPassword,
        String error) {

    public static BulkUserResult success(int row, String firstName, String lastName, String email, Role role,
            String temporaryPassword) {
        return new BulkUserResult(row, firstName, lastName, email, role, true, temporaryPassword, null);
    }

    public static BulkUserResult failure(int row, String firstName, String lastName, String email, Role role,
            String error) {
        return new BulkUserResult(row, firstName, lastName, email, role, false, null, error);
    }
}

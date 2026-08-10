package com.fris.boardportal.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationSignupRequest(
        @NotBlank String organizationName,
        @NotBlank String adminFirstName,
        @NotBlank String adminLastName,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String adminPassword) {
}

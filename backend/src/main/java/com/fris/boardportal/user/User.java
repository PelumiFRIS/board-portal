package com.fris.boardportal.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column
    private String title;

    @Column
    private String phone;

    @Column
    private String bio;

    @Column(name = "calendar_token")
    private String calendarToken;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    /**
     * Additive, not exclusive with role: a BOARD_MEMBER or COMPANY_SECRETARY can
     * also hold Admin rights (e.g. a small org where one person must cover both
     * account administration and content management). See AppUserPrincipal's
     * hasAdminAccess()/getAuthorities() for how this is granted alongside role.
     */
    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin;

    public static User create(UUID organizationId, String email, String passwordHash,
            String firstName, String lastName, Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganizationId(organizationId);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setAdmin(false);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }
}
